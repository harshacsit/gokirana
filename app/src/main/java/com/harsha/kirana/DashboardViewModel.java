package com.harsha.kirana;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.*;

public class DashboardViewModel extends ViewModel {

    private static final String TAG = "KiranaVM";

    private final MutableLiveData<List<Product>> productsLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    // SingleLiveEvent — fires ONCE, never re-delivers to new observers
    // This is the definitive fix for alerts on every screen switch
    private final SingleLiveEvent<Product> lowStockAlert = new SingleLiveEvent<>();
    private final SingleLiveEvent<Product> expiryAlert   = new SingleLiveEvent<>();

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration listenerReg;
    private Context appContext;
    private boolean initialLoadDone = false;

    public DashboardViewModel() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) startListening(user.getUid());
        else              loadSampleData();
    }

    public void setContext(Context ctx) {
        this.appContext = ctx.getApplicationContext();
    }

    private String getUid() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u != null ? u.getUid() : null;
    }

    private void startListening(String uid) {
        if (listenerReg != null) { listenerReg.remove(); listenerReg = null; }
        listenerReg = db.collection("users").document(uid)
                .collection("products")
                .addSnapshotListener((snaps, err) -> {
                    if (err != null) {
                        String msg = err.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED
                                ? "Permission denied — fix Firestore rules"
                                : "Sync error: " + err.getMessage();
                        errorLiveData.postValue(msg);
                        return;
                    }
                    if (snaps == null) return;

                    List<Product> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snaps.getDocuments()) {
                        try {
                            Product p = doc.toObject(Product.class);
                            if (p != null) { p.setId(doc.getId()); list.add(p); }
                        } catch (Exception e) { Log.e(TAG, "Parse: " + e.getMessage()); }
                    }
                    productsLiveData.postValue(list);

                    if (!initialLoadDone) {
                        initialLoadDone = true;
                        if (!AlertSession.get().wasStartupDone()) {
                            AlertSession.get().markStartupDone();
                            runAlertCheck(list, true);
                        }
                    } else {
                        // Only check for NEW crossings (sell/edit) — no push
                        runAlertCheck(list, false);
                    }
                });
    }

    /**
     * ALERT RULES:
     * - Low stock  : qty <= threshold (isLowStock)
     * - Expiry     : 0, 1, 2, 3 days OR already expired (isExpiringSoon || isExpired)
     *   NOT 4,5,6,7 days — isExpiringThisWeek is NOT used for alerts
     * - sendPush   : true only on first app load
     * - Each product alerts AT MOST ONCE per session via AlertSession
     */
    private void runAlertCheck(List<Product> products, boolean sendPush) {
        for (Product p : products) {
            String pid = p.getId();
            if (pid == null) continue;

            // LOW STOCK
            if (p.isLowStock() && !AlertSession.get().isLowStockShown(pid)) {
                AlertSession.get().markLowStockShown(pid);
                lowStockAlert.postValue(p);
                if (sendPush && appContext != null) {
                    List<Product> s = new ArrayList<>(); s.add(p);
                    NotificationHelper.sendLowStockNotification(appContext, s);
                }
            }
            if (!p.isLowStock() && p.getQuantity() > p.getLowStockThreshold() + 3)
                AlertSession.get().clearLowStockShown(pid);

            // EXPIRY — 0 to 3 days only (NOT isExpiringThisWeek)
            boolean needsExpiry = p.isExpired() || p.isExpiringSoon();
            if (needsExpiry && !AlertSession.get().isExpiryShown(pid)) {
                AlertSession.get().markExpiryShown(pid);
                expiryAlert.postValue(p);
                if (sendPush && appContext != null) {
                    List<Product> s = new ArrayList<>(); s.add(p);
                    NotificationHelper.sendExpiryNotification(appContext, s);
                }
            }
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (listenerReg != null) listenerReg.remove();
    }

    public LiveData<List<Product>> getProductsLiveData() { return productsLiveData; }
    public LiveData<String>        getErrorLiveData()    { return errorLiveData; }
    public LiveData<Product>       getLowStockAlert()    { return lowStockAlert; }
    public LiveData<Product>       getExpiryAlert()      { return expiryAlert; }

    private List<Product> getProducts() {
        List<Product> l = productsLiveData.getValue();
        return l != null ? l : new ArrayList<>();
    }

    public void addProduct(String name, int expiryDays, int quantity, int threshold) {
        String uid = getUid();
        if (uid == null) { errorLiveData.postValue("Not logged in"); return; }
        if (name == null || name.trim().isEmpty()) return;
        Map<String, Object> data = new HashMap<>();
        data.put("name",              name.trim());
        data.put("quantity",          Math.max(1, quantity));
        data.put("expiryDate",        expiryDays >= 0 ? Product.daysToDateString(expiryDays) : "");
        data.put("lowStockThreshold", Math.max(1, threshold));
        db.collection("users").document(uid).collection("products").add(data)
                .addOnSuccessListener(r -> Log.d(TAG, "Added: " + r.getId()))
                .addOnFailureListener(e -> errorLiveData.postValue("Save failed: " + e.getMessage()));
    }

    public void editProduct(String productId, String name, int expiryDays,
                            int quantity, int threshold) {
        String uid = getUid();
        if (uid == null || productId == null || name == null || name.trim().isEmpty()) return;
        Map<String, Object> data = new HashMap<>();
        data.put("name",              name.trim());
        data.put("quantity",          Math.max(0, quantity));
        data.put("expiryDate",        expiryDays >= 0 ? Product.daysToDateString(expiryDays) : "");
        data.put("lowStockThreshold", Math.max(1, threshold));
        db.collection("users").document(uid).collection("products")
                .document(productId).update(data)
                .addOnSuccessListener(v -> AlertSession.get().clearLowStockShown(productId))
                .addOnFailureListener(e -> errorLiveData.postValue("Update failed: " + e.getMessage()));
    }

    public void decreaseQty(Product product) {
        if (product.getQuantity() <= 0) {
            errorLiveData.postValue(product.getName() + " is out of stock!"); return;
        }
        int newQty = product.getQuantity() - 1;
        writeQty(product, newQty);
        if (newQty <= product.getLowStockThreshold()
                && !AlertSession.get().isLowStockShown(product.getId())) {
            AlertSession.get().markLowStockShown(product.getId());
            Product u = new Product(product.getName(), newQty,
                    product.getExpiryDate(), product.getLowStockThreshold());
            u.setId(product.getId());
            lowStockAlert.postValue(u);
        }
    }

    public void increaseQty(Product product) {
        writeQty(product, product.getQuantity() + 1);
        if (product.getQuantity() + 1 > product.getLowStockThreshold() + 3)
            AlertSession.get().clearLowStockShown(product.getId());
    }

    private void writeQty(Product product, int qty) {
        String uid = getUid();
        if (uid == null || product.getId() == null) return;
        db.collection("users").document(uid).collection("products")
                .document(product.getId()).update("quantity", qty)
                .addOnFailureListener(e -> Log.e(TAG, "Qty fail: " + e.getMessage()));
    }

    public void removeProduct(Product product) {
        String uid = getUid();
        if (uid == null || product.getId() == null) return;
        AlertSession.get().markLowStockShown(product.getId());
        AlertSession.get().markExpiryShown(product.getId());
        db.collection("users").document(uid).collection("products")
                .document(product.getId()).delete()
                .addOnFailureListener(e -> errorLiveData.postValue("Delete failed: " + e.getMessage()));
    }

    public void markAsHandled(Product product, String alertType) {
        if (product == null || product.getId() == null) return;
        if ("low_stock".equals(alertType)) AlertSession.get().markLowStockShown(product.getId());
        else                               AlertSession.get().markExpiryShown(product.getId());
        String uid = getUid();
        if (uid == null) return;
        Map<String, Object> u = new HashMap<>();
        u.put("lastHandledAt", System.currentTimeMillis());
        u.put("handledAlertType", alertType);
        db.collection("users").document(uid).collection("products")
                .document(product.getId()).update(u)
                .addOnFailureListener(e -> Log.e(TAG, "Mark handled fail: " + e.getMessage()));
    }

    public List<Product> getLowStockItems() {
        List<Product> r = new ArrayList<>();
        for (Product p : getProducts()) if (p.isLowStock()) r.add(p);
        return r;
    }

    public List<Product> getExpiringItems() {
        List<Product> r = new ArrayList<>();
        for (Product p : getProducts()) if (p.isExpiringSoon() || p.isExpired()) r.add(p);
        return r;
    }

    public int totalItems() { return getProducts().size(); }
    public void sendWhatsAppAlert() {}

    private void loadSampleData() {
        List<Product> s = new ArrayList<>();
        s.add(new Product("Amul Milk 1L",  4,  Product.daysToDateString(2),   5));
        s.add(new Product("Tata Salt 1kg", 12, Product.daysToDateString(90),  5));
        s.add(new Product("Maggi Noodles", 24, Product.daysToDateString(150), 5));
        productsLiveData.setValue(s);
    }
}