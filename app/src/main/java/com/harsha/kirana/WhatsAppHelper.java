package com.harsha.kirana;

import android.util.Base64;
import android.util.Log;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

/**
 * WhatsAppHelper — Twilio WhatsApp API
 *
 * SETUP (one time — 5 minutes):
 * 1. twilio.com → Sign Up Free
 * 2. Console Dashboard → copy Account SID and Auth Token
 * 3. Messaging → Try it out → Send a WhatsApp message
 * 4. Note your sandbox number (+14155238886) and join code
 * 5. Paste SID and TOKEN below
 *
 * EVERY NEW USER: after signup the app automatically sends them
 * a welcome WhatsApp message. They must have joined the sandbox
 * once (send "join <code>" to +14155238886 from their WhatsApp).
 *
 * DAILY ALERTS: sent once per day from 8 AM alarm only.
 */
public class WhatsAppHelper {

    // ── FILL THESE IN ──────────────────────────────────────────────────────
    static final String ACCOUNT_SID = "";
    static final String AUTH_TOKEN  = "";
    static final String FROM_PHONE  = "whatsapp";
    // ───────────────────────────────────────────────────────────────────────

    private static final String TAG = "WhatsApp";

    // ── WELCOME MESSAGE — called from SignupActivity after successful signup
    public static void sendWelcomeMessage(String phoneNumber, String userName) {
        String to = normalizePhone(phoneNumber);
        if (to == null) return;

        String msg = buildWelcomeMessage(userName);
        sendViaTwilio("whatsapp:" + to, msg);
        Log.d(TAG, "Welcome message queued for: " + to);
    }

    // ── DAILY ALERT — called from 8 AM alarm only, never from app open ────
    public static void sendLowStockAlert(List<Product> products) {
        if (products == null || products.isEmpty()) return;
        fetchPhoneAndSend("low_stock", products);
    }

    public static void sendExpiryAlert(List<Product> products) {
        if (products == null || products.isEmpty()) return;
        fetchPhoneAndSend("expiry", products);
    }

    // ── Fetch logged-in user's phone from Firestore then send ─────────────
    private static void fetchPhoneAndSend(String type, List<Product> products) {
        com.google.firebase.auth.FirebaseUser user =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { Log.w(TAG, "No user logged in"); return; }

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    String phone = doc.getString("phone");
                    String name  = doc.getString("name");
                    String to    = normalizePhone(phone);
                    if (to == null) { Log.w(TAG, "No phone for user"); return; }
                    String msg = "expiry".equals(type)
                            ? buildExpiryMessage(products, name)
                            : buildLowStockMessage(products, name);
                    sendViaTwilio("whatsapp:" + to, msg);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Fetch phone failed: " + e.getMessage()));
    }

    // ── Normalize phone → +91XXXXXXXXXX ──────────────────────────────────
    private static String normalizePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return null;
        phone = phone.trim().replaceAll("[\\s\\-()]+", "");
        if (!phone.startsWith("+")) {
            if (phone.length() == 10)                             phone = "+91" + phone;
            else if (phone.startsWith("91") && phone.length() == 12) phone = "+" + phone;
            else                                                  phone = "+" + phone;
        }
        return phone;
    }

    // ── Welcome message (English + Telugu) ───────────────────────────────
    private static String buildWelcomeMessage(String name) {
        String n = (name != null && !name.isEmpty()) ? name : "Shopkeeper";
        return "👋 *Welcome to GoKirana, " + n + "!*\n\n"
                + "Your smart kirana store manager is ready.\n\n"
                + "✅ Track stock expiry\n"
                + "✅ Get low stock alerts\n"
                + "✅ Daily WhatsApp updates at 8 AM\n\n"
                + "You will receive daily alerts here when products are expiring or running low.\n\n"
                + "— *GoKirana Team*\n\n"
                + "────────────────\n"
                + "👋 *గో కిరాణాకు స్వాగతం, " + n + "!*\n\n"
                + "మీ స్మార్ట్ కిరాణా స్టోర్ మేనేజర్ సిద్ధంగా ఉంది.\n\n"
                + "✅ స్టాక్ గడువు ట్రాక్ చేయండి\n"
                + "✅ తక్కువ స్టాక్ హెచ్చరికలు పొందండి\n"
                + "✅ ప్రతి రోజు ఉదయం 8 గంటలకు WhatsApp అప్‌డేట్‌లు\n\n"
                + "మీకు రోజూ ఉదయం ఇక్కడ హెచ్చరికలు వస్తాయి.\n\n"
                + "— *GoKirana బృందం*";
    }

    // ── Low stock message (English + Telugu) ─────────────────────────────
    private static String buildLowStockMessage(List<Product> products, String name) {
        String n = (name != null && !name.isEmpty()) ? name : "Shopkeeper";
        StringBuilder msg = new StringBuilder();
        msg.append("🛒 *GoKirana — Daily Alert*\n");
        msg.append("Hello ").append(n).append("!\n\n");
        msg.append("⚠️ *Low Stock Warning*\n");
        for (Product p : products)
            msg.append("• ").append(p.getName())
                    .append(" — only *").append(p.getQuantity()).append(" left*\n");
        msg.append("\nPlease restock today!\n\n");
        msg.append("────────────────\n");
        msg.append("⚠️ *స్టాక్ హెచ్చరిక*\n");
        for (Product p : products)
            msg.append("• ").append(p.getName())
                    .append(" — కేవలం *").append(p.getQuantity()).append(" మాత్రమే*\n");
        msg.append("\nవెంటనే స్టాక్ నింపండి!");
        return msg.toString();
    }

    // ── Expiry message (English + Telugu) ────────────────────────────────
    private static String buildExpiryMessage(List<Product> products, String name) {
        String n = (name != null && !name.isEmpty()) ? name : "Shopkeeper";
        StringBuilder msg = new StringBuilder();
        msg.append("🛒 *GoKirana — Daily Alert*\n");
        msg.append("Hello ").append(n).append("!\n\n");
        msg.append("🗓️ *Expiry Warning*\n");
        for (Product p : products) {
            int d = p.daysUntilExpiry();
            String ds = d < 0 ? "EXPIRED!" : d == 0 ? "TODAY!" : d == 1 ? "Tomorrow!" : "in " + d + " days";
            msg.append("• ").append(p.getName()).append(" — *").append(ds).append("*");
            if (p.getExpiryDate() != null && !p.getExpiryDate().isEmpty())
                msg.append(" (").append(p.getExpiryDate()).append(")");
            msg.append("\n");
        }
        msg.append("\nSell or use before expiry!\n\n");
        msg.append("────────────────\n");
        msg.append("🗓️ *గడువు హెచ్చరిక*\n");
        for (Product p : products) {
            int d = p.daysUntilExpiry();
            String ds = d < 0 ? "గడువు అయిపోయింది!" : d == 0 ? "ఈరోజే!" : d == 1 ? "రేపు!" : d + " రోజుల్లో";
            msg.append("• ").append(p.getName()).append(" — *").append(ds).append("*\n");
        }
        msg.append("\nగడువు ముందు అమ్మండి!");
        return msg.toString();
    }

    // ── Core Twilio API call ──────────────────────────────────────────────
    static void sendViaTwilio(String toPhone, String message) {
        if (ACCOUNT_SID.startsWith("ACxxxxxxx") || AUTH_TOKEN.equals("your_auth_token_here")) {
            Log.w(TAG, "Twilio not configured — fill ACCOUNT_SID and AUTH_TOKEN");
            return;
        }
        new Thread(() -> {
            try {
                String apiUrl = "https://api.twilio.com/2010-04-01/Accounts/"
                        + ACCOUNT_SID + "/Messages.json";
                String creds   = ACCOUNT_SID + ":" + AUTH_TOKEN;
                String auth    = "Basic " + Base64.encodeToString(creds.getBytes("UTF-8"), Base64.NO_WRAP);
                String body    = "To="   + URLEncoder.encode(toPhone,   "UTF-8")
                        + "&From=" + URLEncoder.encode(FROM_PHONE, "UTF-8")
                        + "&Body=" + URLEncoder.encode(message,   "UTF-8");

                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", auth);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                OutputStream os = conn.getOutputStream();
                os.write(body.getBytes("UTF-8"));
                os.flush(); os.close();

                int code = conn.getResponseCode();
                if (code == 201) Log.d(TAG, "✅ WhatsApp sent to " + toPhone);
                else {
                    java.io.InputStream err = conn.getErrorStream();
                    if (err != null) {
                        byte[] buf = new byte[512];
                        int len = err.read(buf);
                        Log.e(TAG, "❌ Twilio " + code + ": " + new String(buf, 0, len));
                    }
                }
                conn.disconnect();
            } catch (Exception e) { Log.e(TAG, "Send error: " + e.getMessage()); }
        }).start();
    }
}