package com.harsha.kirana;

import androidx.appcompat.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import java.util.ArrayList;
import java.util.List;
import static com.harsha.kirana.AppTheme.*;
import static com.harsha.kirana.UiHelper.*;

public class DashboardActivity extends AppCompatActivity {

    private static final int TAB_ALL = 0, TAB_EXPIRING = 1, TAB_LOW = 2;

    // MAX LIMITS — prevents absurd entries
    private static final int MAX_EXPIRY_DAYS = 365;   // 1 year max
    private static final int MAX_QUANTITY    = 999;   // 3 digits max
    private static final int MAX_THRESHOLD   = 500;   // sensible limit

    private DashboardViewModel viewModel;
    private LinearLayout productListContainer;
    private LinearLayout alertContainer;
    private TextView     tvTotal, tvLow, tvExp;
    private EditText     searchField;
    private ProgressBar  loadingBar;
    private int          activeTab   = TAB_ALL;
    private List<Product> allProducts = new ArrayList<>();
    private boolean      firstLoad   = true;

    // Track whether we have already subscribed to alert LiveData this instance
    // This prevents duplicate observers when setContentView is called again (lang toggle)
    private boolean alertObserversAdded = false;

    private String s(String k) { return LangHelper.t(this, k); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        viewModel.setContext(this);

        // Init TTS early — queues message until ready
        VoiceAlertHelper.init(this);

        setContentView(buildUI());
        attachObservers();
    }

    /**
     * Attach LiveData observers ONCE per Activity instance.
     * Calling setContentView again (lang toggle) does NOT re-add observers.
     * SingleLiveEvent ensures each alert fires only once regardless.
     */
    private void attachObservers() {
        if (alertObserversAdded) return;
        alertObserversAdded = true;

        viewModel.getErrorLiveData().observe(this, err -> {
            if (err != null) showOfflineBanner(err);
        });

        viewModel.getProductsLiveData().observe(this, products -> {
            allProducts = products != null ? products : new ArrayList<>();
            if (tvTotal != null) tvTotal.setText(String.valueOf(viewModel.totalItems()));
            if (tvLow   != null) tvLow.setText(String.valueOf(viewModel.getLowStockItems().size()));
            if (tvExp   != null) tvExp.setText(String.valueOf(viewModel.getExpiringItems().size()));
            if (firstLoad) { firstLoad = false; hideLoading(); }
            applyFilter();
        });

        // LOW STOCK — SingleLiveEvent: fires exactly once, never re-delivers
        viewModel.getLowStockAlert().observe(this, p -> {
            if (p == null) return;
            showAlert(
                    "⚠️  " + s("low_stock_alert") + ":  " + p.getName()
                            + "  (" + p.getQuantity() + ")",
                    0xFFFFF3E0, AMBER, p, "low_stock"
            );
            List<Product> list = new ArrayList<>(); list.add(p);
            VoiceAlertHelper.speakLowStockAlert(this, list);
        });

        // EXPIRY — SingleLiveEvent: fires exactly once
        viewModel.getExpiryAlert().observe(this, p -> {
            if (p == null) return;
            int d = p.daysUntilExpiry();
            String msg = d < 0  ? "🔴  " + s("expired")      + ":  " + p.getName()
                    : d == 0 ? "🔴  " + s("expires_today") + ":  " + p.getName()
                    : d == 1 ? "🔴  " + s("expires_tom")   + ":  " + p.getName()
                    :          "🔴  " + s("expires_2d")    + ":  " + p.getName();
            showAlert(msg, 0xFFFFEBEE, RED, p, "expiry");
            List<Product> list = new ArrayList<>(); list.add(p);
            VoiceAlertHelper.speakExpiryAlert(this, list);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VoiceAlertHelper.stop();
    }

    // ── Offline / error banner ─────────────────────────────────────────────
    private void showOfflineBanner(String message) {
        if (alertContainer == null) return;
        alertContainer.removeAllViews();

        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setBackgroundColor(0xFF333333);
        banner.setPadding(dp(this,16), dp(this,12), dp(this,12), dp(this,12));

        TextView tvMsg = new TextView(this);
        tvMsg.setText("📡  " + message);
        tvMsg.setTextColor(0xFFFFFFFF);
        tvMsg.setTextSize(TEXT_SM);
        tvMsg.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvClose = new TextView(this);
        tvClose.setText("✕"); tvClose.setTextColor(0xFFFFFFFF); tvClose.setTextSize(16f);
        tvClose.setPadding(dp(this,10),0,dp(this,4),0);
        tvClose.setClickable(true); tvClose.setFocusable(true);
        tvClose.setOnClickListener(v -> dismissAlert());

        banner.addView(tvMsg); banner.addView(tvClose);
        alertContainer.addView(banner);
        alertContainer.setVisibility(View.VISIBLE);
    }

    // ── Alert banner with Mark as Handled ─────────────────────────────────
    private void showAlert(String msg, int bgColor, int textColor,
                           Product product, String alertType) {
        if (alertContainer == null) return;
        alertContainer.removeAllViews();
        VoiceAlertHelper.stop();

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(bgColor);

        // Row 1: message + close
        LinearLayout msgRow = new LinearLayout(this);
        msgRow.setOrientation(LinearLayout.HORIZONTAL);
        msgRow.setGravity(Gravity.CENTER_VERTICAL);
        msgRow.setPadding(dp(this,16), dp(this,12), dp(this,12), dp(this,4));

        TextView tvMsg = new TextView(this);
        tvMsg.setText(msg); tvMsg.setTextColor(textColor);
        tvMsg.setTextSize(TEXT_MD); tvMsg.setTypeface(Typeface.DEFAULT_BOLD);
        tvMsg.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvX = new TextView(this);
        tvX.setText("✕"); tvX.setTextColor(textColor); tvX.setTextSize(18f);
        tvX.setPadding(dp(this,10),0,dp(this,4),0);
        tvX.setClickable(true); tvX.setFocusable(true);
        tvX.setOnClickListener(v -> { VoiceAlertHelper.stop(); dismissAlert(); });

        msgRow.addView(tvMsg); msgRow.addView(tvX);
        card.addView(msgRow);

        // Row 2: action buttons
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(dp(this,16), 0, dp(this,16), dp(this,12));
        btnRow.setGravity(Gravity.CENTER_VERTICAL);

        // ✅ Mark as Handled — stops alert permanently this session
        LinearLayout btnHandled = mkBtn(
                LangHelper.isTelugu(this) ? "✅ పరిష్కరించాను" : "✅ Mark Handled",
                textColor, bgColor);
        LinearLayout.LayoutParams bp1 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp1.setMargins(0, 0, dp(this,8), 0); btnHandled.setLayoutParams(bp1);
        btnHandled.setOnClickListener(v -> {
            VoiceAlertHelper.stop();
            if (product != null) viewModel.markAsHandled(product, alertType);
            dismissAlert();
            Toast.makeText(this,
                    LangHelper.isTelugu(this) ? "పరిష్కరించబడింది ✅" : "Handled ✅",
                    Toast.LENGTH_SHORT).show();
        });

        // 🔇 Stop Voice
        LinearLayout btnStop = mkBtn(
                LangHelper.isTelugu(this) ? "🔇 ఆపు" : "🔇 Stop",
                textColor, 0x33000000);
        btnStop.setBackground(roundedStroke(0x22000000, textColor, 8, 1, this));
        LinearLayout.LayoutParams bp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp2.setMargins(0, 0, dp(this,8), 0); btnStop.setLayoutParams(bp2);
        btnStop.setOnClickListener(v -> VoiceAlertHelper.stop());

        // 📲 WhatsApp
        LinearLayout btnWA = mkBtn("📲 WhatsApp", 0xFFFFFFFF, 0xFF25D366);
        btnWA.setOnClickListener(v -> {
            if (product == null) return;
            List<Product> wa = new ArrayList<>(); wa.add(product);
            if ("expiry".equals(alertType)) WhatsAppHelper.sendExpiryAlert(wa);
            else                            WhatsAppHelper.sendLowStockAlert(wa);
        });

        btnRow.addView(btnHandled); btnRow.addView(btnStop); btnRow.addView(btnWA);
        card.addView(btnRow);

        alertContainer.addView(card);
        card.setTranslationY(-250f);
        card.animate().translationY(0f).setDuration(320)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        alertContainer.setVisibility(View.VISIBLE);
        new Handler(Looper.getMainLooper()).postDelayed(this::dismissAlert, 10000);
    }

    private LinearLayout mkBtn(String label, int tc, int bg) {
        LinearLayout btn = new LinearLayout(this);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(this,14), dp(this,7), dp(this,14), dp(this,7));
        btn.setBackground(roundedBg(bg, 8, this));
        btn.setClickable(true); btn.setFocusable(true);
        TextView tv = new TextView(this);
        tv.setText(label); tv.setTextColor(tc);
        tv.setTextSize(TEXT_XS); tv.setTypeface(Typeface.DEFAULT_BOLD);
        btn.addView(tv); return btn;
    }

    private void dismissAlert() {
        if (alertContainer == null || alertContainer.getChildCount() == 0) return;
        alertContainer.animate().translationY(-250f).setDuration(260)
                .withEndAction(() -> {
                    alertContainer.setVisibility(View.GONE);
                    alertContainer.removeAllViews();
                    alertContainer.setTranslationY(0f);
                }).start();
    }

    private void hideLoading() {
        if (loadingBar != null)
            loadingBar.animate().alpha(0f).setDuration(300)
                    .withEndAction(() -> loadingBar.setVisibility(View.GONE)).start();
    }

    // ── Build UI ───────────────────────────────────────────────────────────
    private LinearLayout buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG_PAGE);

        alertContainer = new LinearLayout(this);
        alertContainer.setOrientation(LinearLayout.VERTICAL);
        alertContainer.setVisibility(View.GONE);
        root.addView(alertContainer);

        // Blue top bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.VERTICAL);
        topBar.setBackgroundColor(BG_TOPBAR);
        topBar.setPadding(dp(this,18), dp(this,48), dp(this,18), dp(this,14));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View bar = new View(this); bar.setBackgroundColor(0xFFFFFFFF);
        LinearLayout.LayoutParams bP = new LinearLayout.LayoutParams(dp(this,4), dp(this,30));
        bP.setMargins(0,0,dp(this,12),0); bar.setLayoutParams(bP);

        LinearLayout tCol = new LinearLayout(this);
        tCol.setOrientation(LinearLayout.VERTICAL);
        tCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView tvTitle = new TextView(this);
        tvTitle.setText(s("stock_manager")); tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTextSize(TEXT_XL); tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tCol.addView(tvTitle);

        // Language toggle
        TextView langBtn = new TextView(this);
        langBtn.setText(LangHelper.isTelugu(this) ? "EN" : "తె");
        langBtn.setTextColor(0xFFFFFFFF); langBtn.setTextSize(12f);
        langBtn.setTypeface(Typeface.DEFAULT_BOLD);
        langBtn.setPadding(dp(this,12), dp(this,7), dp(this,12), dp(this,7));
        langBtn.setBackground(roundedStroke(0x33FFFFFF, 0xFFFFFFFF, 20, 1, this));
        langBtn.setClickable(true); langBtn.setFocusable(true);
        LinearLayout.LayoutParams lgP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lgP.setMargins(dp(this,10),0,dp(this,10),0); langBtn.setLayoutParams(lgP);
        langBtn.setOnClickListener(v -> {
            LangHelper.toggle(this);
            // Rebuild UI only — do NOT call attachObservers() again
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                firstLoad = true;
                setContentView(buildUI());
                // Manually refresh stats since observers aren't re-added
                List<Product> cur = viewModel.getProductsLiveData().getValue();
                if (cur != null) {
                    allProducts = cur;
                    if (tvTotal != null) tvTotal.setText(String.valueOf(viewModel.totalItems()));
                    if (tvLow   != null) tvLow.setText(String.valueOf(viewModel.getLowStockItems().size()));
                    if (tvExp   != null) tvExp.setText(String.valueOf(viewModel.getExpiringItems().size()));
                    hideLoading();
                    applyFilter();
                }
            }, 100);
        });

        // Add button
        LinearLayout addBtn = new LinearLayout(this);
        addBtn.setGravity(Gravity.CENTER);
        addBtn.setPadding(dp(this,14), dp(this,9), dp(this,14), dp(this,9));
        addBtn.setBackground(roundedBg(0xFFFFFFFF, 12, this));
        addBtn.setClickable(true); addBtn.setFocusable(true);
        addBtn.setOnClickListener(v -> {
            AnimationHelper.pulse(addBtn);
            new Handler(Looper.getMainLooper()).postDelayed(() -> showDialog(null), 100);
        });
        TextView addTv = new TextView(this);
        addTv.setText("+ " + (LangHelper.isTelugu(this) ? "జోడించు" : "Add"));
        addTv.setTextColor(BLUE); addTv.setTextSize(TEXT_SM);
        addTv.setTypeface(Typeface.DEFAULT_BOLD);
        addBtn.addView(addTv);

        topRow.addView(bar); topRow.addView(tCol);
        topRow.addView(langBtn); topRow.addView(addBtn);
        topBar.addView(topRow);

        // Stats chips
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams stP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stP.setMargins(0, dp(this,14), 0, 0); stats.setLayoutParams(stP);
        tvTotal = addChip(stats, "0", s("products"), 0xFFFFFFFF, 0x33FFFFFF, true);
        tvLow   = addChip(stats, "0", s("low_stock"), 0xFFFFECB3, 0x33FFB300, false);
        tvExp   = addChip(stats, "0", s("expiring"),  0xFFFFCDD2, 0x33E53935, false);
        topBar.addView(stats);

        // Search
        searchField = new EditText(this);
        searchField.setHint(s("search")); searchField.setHintTextColor(TEXT_HINT);
        searchField.setTextColor(TEXT_WHITE); searchField.setTextSize(TEXT_MD);
        searchField.setBackground(roundedStroke(BG_CARD, BLACK_BORDER, 10, 1, this));
        searchField.setPadding(dp(this,16), dp(this,12), dp(this,16), dp(this,12));
        LinearLayout.LayoutParams sfP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(this,50));
        sfP.setMargins(0, dp(this,12), 0, 0); searchField.setLayoutParams(sfP);
        searchField.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { applyFilter(); }
            public void afterTextChanged(Editable s) {}
        });
        topBar.addView(searchField);
        topBar.addView(buildTabs());
        root.addView(topBar);

        // List
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG_PAGE);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(BG_PAGE);

        loadingBar = new ProgressBar(this);
        loadingBar.getIndeterminateDrawable()
                .setColorFilter(BLUE, android.graphics.PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams lbP = new LinearLayout.LayoutParams(dp(this,44), dp(this,44));
        lbP.gravity = Gravity.CENTER_HORIZONTAL;
        lbP.setMargins(0, dp(this,60), 0, dp(this,60)); loadingBar.setLayoutParams(lbP);
        content.addView(loadingBar);

        productListContainer = new LinearLayout(this);
        productListContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(productListContainer);
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(this,80)));
        content.addView(spacer);
        scroll.addView(content);
        root.addView(scroll);

        String[] labels = {"Home", s("stock"), s("analytics"), s("settings")};
        String[] icons  = {"🏠","📦","📊","⚙️"};
        Runnable[] clicks = {
                () -> { startActivity(new Intent(this, HomeActivity.class)); finish(); },
                null,
                () -> startActivity(new Intent(this, AnalyticsActivity.class)),
                () -> startActivity(new Intent(this, SettingsActivity.class))
        };
        root.addView(bottomNav(this, labels, icons, 1, clicks));
        return root;
    }

    private LinearLayout buildTabs() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(this,10), 0, 0); row.setLayoutParams(p);
        String[] labels = {s("all"), s("expiring"), s("low_stock")};
        int[]    ids    = {TAB_ALL, TAB_EXPIRING, TAB_LOW};
        for (int i = 0; i < labels.length; i++) {
            final int idx = ids[i];
            TextView tab = new TextView(this);
            tab.setText(labels[i]); tab.setTextSize(TEXT_SM);
            tab.setTypeface(Typeface.DEFAULT_BOLD); tab.setGravity(Gravity.CENTER);
            tab.setPadding(dp(this,16), dp(this,8), dp(this,16), dp(this,8));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tp.setMargins(0,0,dp(this,8),0); tab.setLayoutParams(tp);
            tab.setClickable(true); tab.setFocusable(true);
            styleTab(tab, i == 0);
            tab.setOnClickListener(v -> {
                activeTab = idx;
                for (int j = 0; j < row.getChildCount(); j++) {
                    View c = row.getChildAt(j);
                    if (c instanceof TextView) styleTab((TextView)c, j == idx);
                }
                applyFilter();
            });
            row.addView(tab);
        }
        return row;
    }

    private void styleTab(TextView t, boolean active) {
        if (active) { t.setBackground(roundedBg(0xFFFFFFFF,20,this)); t.setTextColor(BLUE); }
        else { t.setBackground(roundedStroke(0x33FFFFFF,0xAAFFFFFF,20,1,this)); t.setTextColor(0xCCFFFFFF); }
    }

    private void applyFilter() {
        if (productListContainer == null) return;
        String q = searchField != null ? searchField.getText().toString().trim().toLowerCase() : "";
        List<Product> filtered = new ArrayList<>();
        for (Product p : allProducts) {
            boolean ok = activeTab == TAB_ALL
                    || (activeTab == TAB_EXPIRING && (p.isExpiringSoon() || p.isExpired()))
                    || (activeTab == TAB_LOW && p.isLowStock());
            if (ok && (q.isEmpty() || p.getName().toLowerCase().contains(q))) filtered.add(p);
        }
        refreshList(filtered);
    }

    private void refreshList(List<Product> products) {
        if (productListContainer == null) return;
        productListContainer.removeAllViews();
        TextView lbl = new TextView(this);
        lbl.setText(activeTab == TAB_EXPIRING ? s("expiring").toUpperCase()+" ("+products.size()+")"
                : activeTab == TAB_LOW      ? s("low_stock").toUpperCase()+" ("+products.size()+")"
                : s("products").toUpperCase()+" ("+products.size()+")");
        lbl.setTextColor(TEXT_MUTED); lbl.setTextSize(TEXT_XS);
        lbl.setTypeface(Typeface.DEFAULT_BOLD); lbl.setLetterSpacing(0.1f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(this,16),dp(this,14),dp(this,16),dp(this,6)); lbl.setLayoutParams(lp);
        productListContainer.addView(lbl);
        if (products.isEmpty()) { productListContainer.addView(buildEmpty()); return; }
        for (int i = 0; i < products.size(); i++) {
            View card = buildCard(products.get(i));
            card.setAlpha(0f); card.setTranslationY(30f);
            productListContainer.addView(card);
            card.animate().alpha(1f).translationY(0f).setDuration(200)
                    .setStartDelay(i * 30L)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        }
    }

    private LinearLayout buildCard(Product p) {
        int accent = p.accentColor(), dim = p.accentDimColor();
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(roundedStroke(BG_CARD, BLACK_BORDER, 16, 1, this));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.setMargins(dp(this,14),0,dp(this,14),dp(this,12)); card.setLayoutParams(cp);

        LinearLayout mainRow = new LinearLayout(this);
        mainRow.setOrientation(LinearLayout.HORIZONTAL);
        mainRow.setGravity(Gravity.CENTER_VERTICAL);
        mainRow.setBackgroundColor(BG_CARD);

        View colorBar = new View(this);
        colorBar.setBackground(roundedBg(accent,4,this));
        LinearLayout.LayoutParams cbP = new LinearLayout.LayoutParams(dp(this,5),dp(this,CARD_MIN_HEIGHT));
        cbP.setMargins(0,0,dp(this,14),0); colorBar.setLayoutParams(cbP);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(0,dp(this,14),0,dp(this,10));
        info.setBackgroundColor(BG_CARD);
        info.setLayoutParams(new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));

        TextView tvName = new TextView(this);
        tvName.setText(p.getName()); tvName.setTextColor(TEXT_WHITE);
        tvName.setTextSize(TEXT_LG); tvName.setTypeface(Typeface.DEFAULT_BOLD);

        TextView tvBadge = new TextView(this);
        tvBadge.setText(getStatus(p)); tvBadge.setTextColor(accent);
        tvBadge.setTextSize(TEXT_SM); tvBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvBadge.setBackground(roundedBg(dim,20,this));
        tvBadge.setPadding(dp(this,10),dp(this,4),dp(this,10),dp(this,4));
        LinearLayout.LayoutParams bdP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bdP.setMargins(0,dp(this,6),0,dp(this,6)); tvBadge.setLayoutParams(bdP);

        TextView tvMeta = new TextView(this);
        StringBuilder m = new StringBuilder();
        if (p.getExpiryDate() != null && !p.getExpiryDate().isEmpty()) {
            int d = p.daysUntilExpiry();
            m.append(p.getExpiryDate());
            if (d != Integer.MAX_VALUE) {
                if (d==0) m.append("  (Today)");
                else if (d>0) m.append("  (").append(d).append("d)");
                else m.append("  (").append(Math.abs(d)).append("d ago)");
            }
            m.append("\n");
        }
        m.append(s("stock")).append(": ").append(p.getQuantity());
        tvMeta.setText(m.toString()); tvMeta.setTextColor(TEXT_MUTED);
        tvMeta.setTextSize(TEXT_MD); tvMeta.setTypeface(Typeface.DEFAULT);
        info.addView(tvName); info.addView(tvBadge); info.addView(tvMeta);

        LinearLayout ctrl = new LinearLayout(this);
        ctrl.setOrientation(LinearLayout.VERTICAL); ctrl.setGravity(Gravity.CENTER);
        ctrl.setBackgroundColor(BG_CARD);
        ctrl.setPadding(dp(this,6),dp(this,10),dp(this,14),dp(this,10));

        LinearLayout sellBtn = ctrlBtn("- "+s("sell"), RED, RED_DIM);
        sellBtn.setOnClickListener(v -> { AnimationHelper.pulse(sellBtn); viewModel.decreaseQty(p); });

        TextView tvQty = new TextView(this);
        tvQty.setText(String.valueOf(p.getQuantity()));
        tvQty.setTextColor(p.getQuantity()==0 ? RED : TEXT_WHITE);
        tvQty.setTextSize(22f); tvQty.setTypeface(Typeface.DEFAULT_BOLD); tvQty.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams qP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        qP.setMargins(0,dp(this,6),0,dp(this,6)); tvQty.setLayoutParams(qP);

        LinearLayout buyBtn = ctrlBtn("+ "+s("buy"), GREEN, GREEN_DIM);
        buyBtn.setOnClickListener(v -> { AnimationHelper.pulse(buyBtn); viewModel.increaseQty(p); });

        ctrl.addView(sellBtn); ctrl.addView(tvQty); ctrl.addView(buyBtn);
        mainRow.addView(colorBar); mainRow.addView(info); mainRow.addView(ctrl);
        card.addView(mainRow);

        View div = new View(this);
        div.setBackgroundColor(BLACK_BORDER);
        div.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,1));
        card.addView(div);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL); actions.setBackgroundColor(BG_CARD);
        actions.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout editBtn = new LinearLayout(this); editBtn.setGravity(Gravity.CENTER);
        editBtn.setPadding(0,dp(this,10),0,dp(this,10)); editBtn.setBackgroundColor(BG_CARD);
        editBtn.setLayoutParams(new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        editBtn.setClickable(true); editBtn.setFocusable(true);
        editBtn.setOnClickListener(v -> {
            AnimationHelper.pulse(editBtn);
            new Handler(Looper.getMainLooper()).postDelayed(() -> showDialog(p), 100);
        });
        TextView tvEdit = new TextView(this);
        tvEdit.setText("✏️  "+s("edit")); tvEdit.setTextColor(BLUE);
        tvEdit.setTextSize(TEXT_SM); tvEdit.setTypeface(Typeface.DEFAULT_BOLD);
        editBtn.addView(tvEdit);

        View vDiv = new View(this); vDiv.setBackgroundColor(BLACK_BORDER);
        vDiv.setLayoutParams(new LinearLayout.LayoutParams(1,dp(this,34)));

        LinearLayout removeBtn = new LinearLayout(this); removeBtn.setGravity(Gravity.CENTER);
        removeBtn.setPadding(0,dp(this,10),0,dp(this,10)); removeBtn.setBackgroundColor(BG_CARD);
        removeBtn.setLayoutParams(new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        removeBtn.setClickable(true); removeBtn.setFocusable(true);
        removeBtn.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(s("remove_confirm"))
                        .setMessage("\""+p.getName()+"\" "+s("remove_msg"))
                        .setPositiveButton(s("delete"),(d2,w)->viewModel.removeProduct(p))
                        .setNegativeButton(s("cancel"),null).show());
        TextView tvRemove = new TextView(this);
        tvRemove.setText("🗑️  "+s("remove")); tvRemove.setTextColor(RED);
        tvRemove.setTextSize(TEXT_SM); tvRemove.setTypeface(Typeface.DEFAULT_BOLD);
        removeBtn.addView(tvRemove);

        actions.addView(editBtn); actions.addView(vDiv); actions.addView(removeBtn);
        card.addView(actions);
        return card;
    }

    private String getStatus(Product p) {
        int d = p.daysUntilExpiry();
        if (d==Integer.MAX_VALUE) return p.isLowStock() ? s("low_stock") : s("in_stock");
        if (d<0)  return s("expired")+" ("+Math.abs(d)+"d)";
        if (d==0) return s("expires_today");
        if (d==1) return s("expires_tom");
        if (d==2) return s("expires_2d");
        if (d<=3) return LangHelper.isTelugu(this) ? "3 రోజుల్లో గడువు" : "Expires in 3 days";
        return p.isLowStock() ? s("low_stock") : s("in_stock");
    }

    private LinearLayout ctrlBtn(String label, int tc, int bg) {
        LinearLayout btn = new LinearLayout(this); btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(this,10),dp(this,8),dp(this,10),dp(this,8));
        btn.setBackground(roundedBg(bg,8,this)); btn.setClickable(true); btn.setFocusable(true);
        btn.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView tv = new TextView(this); tv.setText(label); tv.setTextColor(tc);
        tv.setTextSize(TEXT_SM); tv.setTypeface(Typeface.DEFAULT_BOLD); tv.setGravity(Gravity.CENTER);
        btn.addView(tv); return btn;
    }

    private LinearLayout buildEmpty() {
        LinearLayout e = new LinearLayout(this); e.setOrientation(LinearLayout.VERTICAL);
        e.setGravity(Gravity.CENTER); e.setBackgroundColor(BG_PAGE);
        e.setPadding(dp(this,24),dp(this,80),dp(this,24),dp(this,80));
        TextView icon = new TextView(this); icon.setText("📦"); icon.setTextSize(56f); icon.setGravity(Gravity.CENTER);
        TextView msg = new TextView(this);
        msg.setText(activeTab==TAB_EXPIRING ? s("expiring") : activeTab==TAB_LOW ? s("low_stock") : s("no_products"));
        msg.setTextColor(TEXT_MUTED); msg.setTextSize(TEXT_LG); msg.setTypeface(Typeface.DEFAULT_BOLD); msg.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams mP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mP.setMargins(0,dp(this,12),0,dp(this,6)); msg.setLayoutParams(mP);
        TextView hint = new TextView(this); hint.setText(s("add_hint"));
        hint.setTextColor(TEXT_HINT); hint.setTextSize(TEXT_MD); hint.setGravity(Gravity.CENTER);
        e.addView(icon); e.addView(msg); e.addView(hint); return e;
    }

    // ── Add / Edit dialog with MAX LIMITS ─────────────────────────────────
    private void showDialog(Product existing) {
        boolean isEdit = existing != null;
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(BG_CARD);
        layout.setPadding(dp(this,20),dp(this,24),dp(this,20),dp(this,28));

        TextView title = new TextView(this);
        title.setText(isEdit ? s("edit_product") : s("add_product"));
        title.setTextColor(TEXT_WHITE); title.setTextSize(TEXT_XL); title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tP.setMargins(0,0,0,dp(this,20)); title.setLayoutParams(tP);
        layout.addView(title);

        // Product name
        layout.addView(dlabel(s("product_name")));
        EditText etName = dfield(s("product_name")+"...",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        etName.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(50) }); // max 50 chars
        if (isEdit) etName.setText(existing.getName());
        layout.addView(etName);

        // Expiry days with live preview + MAX LIMIT
        layout.addView(dlabel(s("expiry_days")));
        LinearLayout exRow = new LinearLayout(this);
        exRow.setOrientation(LinearLayout.HORIZONTAL); exRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams erP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        erP.setMargins(0,0,0,dp(this,4)); exRow.setLayoutParams(erP);

        EditText etDays = new EditText(this);
        etDays.setHint("1 – "+MAX_EXPIRY_DAYS); etDays.setHintTextColor(TEXT_HINT);
        etDays.setTextColor(TEXT_WHITE); etDays.setTextSize(TEXT_LG); etDays.setTypeface(Typeface.DEFAULT_BOLD);
        etDays.setInputType(InputType.TYPE_CLASS_NUMBER);
        etDays.setBackground(roundedStroke(BG_FIELD, BLACK_BORDER, 10, 1, this));
        etDays.setPadding(dp(this,16),dp(this,14),dp(this,14),dp(this,14));
        // Limit to 3 digits
        etDays.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(3) });
        LinearLayout.LayoutParams dP = new LinearLayout.LayoutParams(0,dp(this,56),1f);
        dP.setMargins(0,0,dp(this,10),0); etDays.setLayoutParams(dP);
        if (isEdit && existing.getExpiryDate() != null && !existing.getExpiryDate().isEmpty()) {
            int dl = existing.daysUntilExpiry();
            if (dl != Integer.MAX_VALUE && dl >= 0) etDays.setText(String.valueOf(dl));
        }

        TextView preview = new TextView(this);
        preview.setText("No expiry"); preview.setTextColor(TEXT_MUTED);
        preview.setTextSize(TEXT_SM); preview.setGravity(Gravity.CENTER);
        preview.setBackground(roundedBg(BG_FIELD,10,this));
        preview.setPadding(dp(this,12),dp(this,12),dp(this,12),dp(this,12));
        preview.setLayoutParams(new LinearLayout.LayoutParams(0,dp(this,56),1f));

        etDays.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence cs, int a, int b, int c) {
                try {
                    int days = Integer.parseInt(cs.toString().trim());
                    if (days > MAX_EXPIRY_DAYS) {
                        etDays.setText(String.valueOf(MAX_EXPIRY_DAYS));
                        etDays.setSelection(etDays.getText().length());
                        preview.setText("Max "+MAX_EXPIRY_DAYS+" days");
                        preview.setTextColor(RED); return;
                    }
                    if (days < 0) { days = 0; }
                    preview.setText(Product.daysToDateString(days) + "\n(" + days + "d)");
                    preview.setTextColor(days<=3 ? RED : days<=30 ? AMBER : GREEN);
                } catch (Exception e) { preview.setText("No expiry"); preview.setTextColor(TEXT_MUTED); }
            }
            public void afterTextChanged(Editable s) {}
        });
        exRow.addView(etDays); exRow.addView(preview); layout.addView(exRow);

        TextView exHint = new TextView(this);
        exHint.setText("Max "+MAX_EXPIRY_DAYS+" days · "+s("expiry_hint"));
        exHint.setTextColor(TEXT_HINT); exHint.setTextSize(TEXT_XS);
        LinearLayout.LayoutParams ehP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ehP.setMargins(0,dp(this,4),0,dp(this,16)); exHint.setLayoutParams(ehP);
        layout.addView(exHint);

        // Quantity with MAX LIMIT
        layout.addView(dlabel(s("quantity")+" (max "+MAX_QUANTITY+")"));
        EditText etQty = dfield("1 – "+MAX_QUANTITY, InputType.TYPE_CLASS_NUMBER);
        etQty.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(3) });
        if (isEdit) etQty.setText(String.valueOf(existing.getQuantity()));
        layout.addView(etQty);

        // Alert threshold with MAX LIMIT
        layout.addView(dlabel(s("alert_at")+" (max "+MAX_THRESHOLD+")"));
        EditText etThr = dfield("1 – "+MAX_THRESHOLD, InputType.TYPE_CLASS_NUMBER);
        etThr.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(3) });
        if (isEdit) etThr.setText(String.valueOf(existing.getLowStockThreshold()));
        layout.addView(etThr);

        Button saveBtn = new Button(this);
        saveBtn.setText(isEdit ? s("update_product") : s("save_product"));
        saveBtn.setTextColor(0xFFFFFFFF); saveBtn.setTextSize(TEXT_MD);
        saveBtn.setTypeface(Typeface.DEFAULT_BOLD);
        saveBtn.setBackground(roundedBg(BLUE,14,this)); saveBtn.setAllCaps(false);
        LinearLayout.LayoutParams sbP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(this,56));
        sbP.setMargins(0,dp(this,8),0,0); saveBtn.setLayoutParams(sbP);
        saveBtn.setOnClickListener(v -> {
            String n = etName.getText().toString().trim();
            if (n.isEmpty()) {
                Toast.makeText(this, s("product_name")+"!", Toast.LENGTH_SHORT).show();
                AnimationHelper.shake(etName); return;
            }
            int days = -1, qty = 1, thr = 5;
            try { days = Math.min(Integer.parseInt(etDays.getText().toString().trim()), MAX_EXPIRY_DAYS); }
            catch (Exception ignored) {}
            try { qty = Math.min(Math.max(1, Integer.parseInt(etQty.getText().toString().trim())), MAX_QUANTITY); }
            catch (Exception ignored) {}
            try { thr = Math.min(Math.max(1, Integer.parseInt(etThr.getText().toString().trim())), MAX_THRESHOLD); }
            catch (Exception ignored) {}

            // Validate threshold < quantity
            if (thr >= qty && qty > 0) {
                Toast.makeText(this,
                        LangHelper.isTelugu(this)
                                ? "హెచ్చరిక పరిమితి పరిమాణం కంటే తక్కువగా ఉండాలి"
                                : "Alert limit must be less than quantity",
                        Toast.LENGTH_SHORT).show();
                AnimationHelper.shake(etThr); return;
            }

            if (isEdit) {
                viewModel.editProduct(existing.getId(), n, days, qty, thr);
                Toast.makeText(this, n+" updated!", Toast.LENGTH_SHORT).show();
            } else {
                viewModel.addProduct(n, days, qty, thr);
                Toast.makeText(this, n+" added!", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        layout.addView(saveBtn);

        ScrollView sv = new ScrollView(this); sv.setBackgroundColor(BG_CARD); sv.addView(layout);
        dialog.setContentView(sv);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setGravity(Gravity.BOTTOM);
        }
        dialog.show();
    }

    private TextView dlabel(String t) {
        TextView tv = new TextView(this); tv.setText(t); tv.setTextColor(TEXT_MUTED);
        tv.setTextSize(TEXT_SM); tv.setTypeface(Typeface.DEFAULT);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0,0,0,dp(this,6)); tv.setLayoutParams(p); return tv;
    }

    private EditText dfield(String hint, int inputType) {
        EditText et = new EditText(this); et.setHint(hint); et.setHintTextColor(TEXT_HINT);
        et.setTextColor(TEXT_WHITE); et.setTextSize(TEXT_MD); et.setTypeface(Typeface.DEFAULT);
        et.setInputType(inputType);
        et.setBackground(roundedStroke(BG_FIELD, BLACK_BORDER, 10, 1, this));
        et.setPadding(dp(this,16),dp(this,14),dp(this,14),dp(this,14));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(this,54));
        p.setMargins(0,0,0,dp(this,14)); et.setLayoutParams(p); return et;
    }

    private TextView addChip(LinearLayout parent, String val, String lbl, int vc, int bg, boolean first) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.VERTICAL); chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(this,12),dp(this,10),dp(this,12),dp(this,10));
        chip.setBackground(roundedBg(bg,10,this));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);
        if (!first) cp.setMargins(dp(this,8),0,0,0); chip.setLayoutParams(cp);
        TextView v = new TextView(this); v.setText(val); v.setTextColor(vc);
        v.setTextSize(22f); v.setTypeface(Typeface.DEFAULT_BOLD); v.setGravity(Gravity.CENTER);
        TextView l = new TextView(this); l.setText(lbl); l.setTextColor(0xAAFFFFFF);
        l.setTextSize(TEXT_XS); l.setTypeface(Typeface.DEFAULT); l.setGravity(Gravity.CENTER);
        chip.addView(v); chip.addView(l); parent.addView(chip); return v;
    }
}