package com.harsha.kirana;

import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import java.util.*;
import static com.harsha.kirana.AppTheme.*;
import static com.harsha.kirana.UiHelper.*;

public class AnalyticsActivity extends AppCompatActivity {

    private DashboardViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        viewModel.setContext(this);
        setContentView(buildUI());
        viewModel.getProductsLiveData().observe(this, products -> {
            // Rebuild to refresh data
            if (products != null) {
                setContentView(buildUI());
            }
        });
    }

    private LinearLayout buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG_PAGE);   // light blue

        // ── Blue top bar ──────────────────────────────────────────────────
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.VERTICAL);
        topBar.setBackgroundColor(BG_TOPBAR);   // solid blue
        topBar.setPadding(dp(this,18), dp(this,48), dp(this,18), dp(this,20));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        View accent = new View(this);
        accent.setBackgroundColor(0xFFFFFFFF);
        LinearLayout.LayoutParams aP = new LinearLayout.LayoutParams(dp(this,4), dp(this,30));
        aP.setMargins(0, 0, dp(this,12), 0);
        accent.setLayoutParams(aP);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        android.widget.TextView titleTv = new android.widget.TextView(this);
        titleTv.setText("Analytics");
        titleTv.setTextColor(0xFFFFFFFF);
        titleTv.setTextSize(TEXT_XL);
        titleTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        android.widget.TextView subTv = new android.widget.TextView(this);
        subTv.setText(new java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
                .format(new Date()) + " · Overview");
        subTv.setTextColor(0xCCFFFFFF);
        subTv.setTextSize(12f);

        textCol.addView(titleTv);
        textCol.addView(subTv);
        topRow.addView(accent);
        topRow.addView(textCol);
        topBar.addView(topRow);
        root.addView(topBar);

        // ── Scrollable content — light blue background ────────────────────
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG_PAGE);   // ← light blue NOT black
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(BG_PAGE);   // ← light blue NOT black
        content.setPadding(dp(this,12), dp(this,8), dp(this,12), dp(this,80));

        List<Product> products = viewModel.getProductsLiveData().getValue();
        if (products == null) products = new ArrayList<>();

        int total   = products.size();
        int low     = viewModel.getLowStockItems().size();
        int expiring= viewModel.getExpiringItems().size();
        int healthy = total - low - expiring;
        if (healthy < 0) healthy = 0;

        // ── Summary cards — 2x2 grid ──────────────────────────────────────
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams r1P = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        r1P.setMargins(0, dp(this,8), 0, dp(this,8));
        row1.setLayoutParams(r1P);

        row1.addView(summaryCard("📦", String.valueOf(total),   "Total Products", BLUE,   BLUE_DIM));
        row1.addView(summaryCard("✅", String.valueOf(healthy), "Healthy Stock",  GREEN,  GREEN_DIM));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams r2P = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        r2P.setMargins(0, 0, 0, dp(this,8));
        row2.setLayoutParams(r2P);
        row2.addView(summaryCard("⚠️", String.valueOf(low),      "Low Stock",    AMBER, AMBER_DIM));
        row2.addView(summaryCard("📅", String.valueOf(expiring), "Expiring Soon", RED,  RED_DIM));

        content.addView(row1);
        content.addView(row2);

        // ── Bar chart — white card ─────────────────────────────────────────
        if (!products.isEmpty()) {
            LinearLayout chartCard = whiteCard();
            android.widget.TextView chartTitle = cardTitle("Stock Levels — Top Products");
            chartCard.addView(chartTitle);

            List<Product> sorted = new ArrayList<>(products);
            sorted.sort((a, b) -> b.getQuantity() - a.getQuantity());
            int max = sorted.isEmpty() ? 1 : Math.max(sorted.get(0).getQuantity(), 1);

            for (int i = 0; i < Math.min(5, sorted.size()); i++) {
                Product p = sorted.get(i);
                chartCard.addView(buildBar(p.getName(), p.getQuantity(), max, p.accentColor()));
            }
            content.addView(chartCard);
        }

        // ── Expiry timeline — white card ───────────────────────────────────
        LinearLayout exCard = whiteCard();
        exCard.addView(cardTitle("📅  Expiry Timeline"));

        List<Product> thisWeek  = new ArrayList<>();
        List<Product> thisMonth = new ArrayList<>();
        List<Product> later     = new ArrayList<>();
        List<Product> expired   = new ArrayList<>();

        for (Product p : products) {
            int days = p.daysUntilExpiry();
            if (days == Integer.MAX_VALUE) continue;
            if (days < 0)        expired.add(p);
            else if (days <= 7)  thisWeek.add(p);
            else if (days <= 30) thisMonth.add(p);
            else                 later.add(p);
        }

        if (!expired.isEmpty())  { exCard.addView(exGroup("🔴  Expired (" + expired.size() + ")",   RED));   for (Product p : expired)   exCard.addView(exRow(p)); }
        if (!thisWeek.isEmpty()) { exCard.addView(exGroup("🔴  This Week 0-7d (" + thisWeek.size() + ")",  RED));   for (Product p : thisWeek)  exCard.addView(exRow(p)); }
        if (!thisMonth.isEmpty()){ exCard.addView(exGroup("🟡  This Month 8-30d (" + thisMonth.size() + ")", AMBER)); for (Product p : thisMonth) exCard.addView(exRow(p)); }
        if (!later.isEmpty())    { exCard.addView(exGroup("🟢  Later (" + later.size() + ")",          GREEN)); for (Product p : later)     exCard.addView(exRow(p)); }

        if (expired.isEmpty() && thisWeek.isEmpty() && thisMonth.isEmpty() && later.isEmpty()) {
            android.widget.TextView empty = new android.widget.TextView(this);
            empty.setText("No expiry dates set");
            empty.setTextColor(TEXT_MUTED); empty.setTextSize(TEXT_MD);
            empty.setPadding(0, dp(this,8), 0, dp(this,8));
            exCard.addView(empty);
        }
        content.addView(exCard);

        // ── Restock card — white card ──────────────────────────────────────
        List<Product> restockList = viewModel.getLowStockItems();
        if (!restockList.isEmpty()) {
            LinearLayout restockCard = whiteCard();
            restockCard.addView(cardTitle("⚠️  Restock Needed (" + restockList.size() + " items)"));
            for (Product p : restockList) {
                LinearLayout rRow = new LinearLayout(this);
                rRow.setOrientation(LinearLayout.HORIZONTAL);
                rRow.setGravity(Gravity.CENTER_VERTICAL);
                rRow.setPadding(0, dp(this,8), 0, dp(this,8));
                rRow.setBackground(roundedBg(RED_DIM, 8, this));
                LinearLayout.LayoutParams rrP = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rrP.setMargins(0, dp(this,4), 0, 0);
                rRow.setLayoutParams(rrP);

                android.widget.TextView rName = new android.widget.TextView(this);
                rName.setText("  " + p.getName());
                rName.setTextColor(TEXT_WHITE); rName.setTextSize(TEXT_MD);
                rName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                rName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                android.widget.TextView rQty = new android.widget.TextView(this);
                rQty.setText("Only " + p.getQuantity() + " left  ");
                rQty.setTextColor(RED); rQty.setTextSize(TEXT_SM);
                rQty.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

                rRow.addView(rName); rRow.addView(rQty);
                restockCard.addView(rRow);
            }
            content.addView(restockCard);
        }

        scroll.addView(content);
        root.addView(scroll);

        // Bottom nav — white
        String[] labels = {"Home","Stock","Analytics","Settings"};
        String[] icons  = {"🏠","📦","📊","⚙️"};
        Runnable[] clicks = {
                () -> { startActivity(new Intent(this, HomeActivity.class)); finish(); },
                () -> { startActivity(new Intent(this, DashboardActivity.class)); finish(); },
                null,
                () -> { startActivity(new Intent(this, SettingsActivity.class)); finish(); }
        };
        root.addView(bottomNav(this, labels, icons, 2, clicks));
        return root;
    }

    // ── Summary card — white bg ────────────────────────────────────────────
    private LinearLayout summaryCard(String icon, String value, String label,
                                     int color, int dimColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(this,14), dp(this,14), dp(this,14), dp(this,14));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(dp(this,4), 0, dp(this,4), 0);
        card.setLayoutParams(p);
        card.setBackground(roundedStroke(BG_CARD, BLACK_BORDER, 14, 1, this));  // WHITE card

        android.widget.TextView iconTv = new android.widget.TextView(this);
        iconTv.setText(icon); iconTv.setTextSize(26f);
        LinearLayout.LayoutParams iP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        iP.setMargins(0, 0, 0, dp(this,8));
        iconTv.setLayoutParams(iP);

        android.widget.TextView valTv = new android.widget.TextView(this);
        valTv.setText(value); valTv.setTextColor(color);
        valTv.setTextSize(28f); valTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        android.widget.TextView lblTv = new android.widget.TextView(this);
        lblTv.setText(label); lblTv.setTextColor(TEXT_MUTED);
        lblTv.setTextSize(TEXT_XS); lblTv.setTypeface(android.graphics.Typeface.DEFAULT);

        card.addView(iconTv); card.addView(valTv); card.addView(lblTv);
        return card;
    }

    // ── Bar chart row ──────────────────────────────────────────────────────
    private LinearLayout buildBar(String name, int qty, int max, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, dp(this,6), 0, 0);
        row.setLayoutParams(rp);

        LinearLayout labelRow = new LinearLayout(this);
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        labelRow.setGravity(Gravity.CENTER_VERTICAL);
        labelRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        android.widget.TextView nameTv = new android.widget.TextView(this);
        nameTv.setText(name); nameTv.setTextColor(TEXT_WHITE);
        nameTv.setTextSize(TEXT_SM); nameTv.setTypeface(android.graphics.Typeface.DEFAULT);
        nameTv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        android.widget.TextView qtyTv = new android.widget.TextView(this);
        qtyTv.setText(String.valueOf(qty)); qtyTv.setTextColor(color);
        qtyTv.setTextSize(TEXT_SM); qtyTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        labelRow.addView(nameTv); labelRow.addView(qtyTv);
        row.addView(labelRow);

        // Bar track — light blue bg
        LinearLayout track = new LinearLayout(this);
        track.setBackground(roundedBg(BG_FIELD, 6, this));   // light blue track
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(this,10));
        tp.setMargins(0, dp(this,4), 0, 0);
        track.setLayoutParams(tp);

        View fill = new View(this);
        fill.setBackground(roundedBg(color, 6, this));
        int pct = Math.max(1, (int)((float)qty / max * 100));
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, pct);
        fill.setLayoutParams(fp);
        track.addView(fill);

        // Empty fill
        View empty = new View(this);
        empty.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 100 - pct));
        track.addView(empty);

        row.addView(track);
        return row;
    }

    // ── White card container ───────────────────────────────────────────────
    private LinearLayout whiteCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(this,16), dp(this,14), dp(this,16), dp(this,16));
        card.setBackground(roundedStroke(BG_CARD, BLACK_BORDER, 14, 1, this));   // WHITE
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(dp(this,4), 0, dp(this,4), dp(this,10));
        card.setLayoutParams(p);
        return card;
    }

    private android.widget.TextView cardTitle(String text) {
        android.widget.TextView tv = new android.widget.TextView(this);
        tv.setText(text); tv.setTextColor(TEXT_WHITE);
        tv.setTextSize(TEXT_MD); tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, dp(this,10));
        tv.setLayoutParams(p);
        return tv;
    }

    private LinearLayout exGroup(String label, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, dp(this,8), 0, dp(this,4));
        row.setLayoutParams(rp);

        android.widget.TextView tv = new android.widget.TextView(this);
        tv.setText(label); tv.setTextColor(color);
        tv.setTextSize(TEXT_SM); tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        row.addView(tv);
        return row;
    }

    private LinearLayout exRow(Product p) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(this,10), dp(this,8), dp(this,10), dp(this,8));
        row.setBackground(roundedBg(BG_FIELD, 8, this));   // light blue row bg
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, 0, 0, dp(this,4));
        row.setLayoutParams(rp);

        android.widget.TextView name = new android.widget.TextView(this);
        name.setText(p.getName()); name.setTextColor(TEXT_WHITE);
        name.setTextSize(TEXT_SM); name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        android.widget.TextView status = new android.widget.TextView(this);
        int d = p.daysUntilExpiry();
        String statusStr = d < 0 ? "▲ Expired" : d == 0 ? "▲ Expires TODAY!" : d == 1 ? "▲ Expires TOMORROW!" : d + " days left";
        int statusColor  = d <= 1 ? RED : d <= 7 ? AMBER : GREEN;
        status.setText(statusStr); status.setTextColor(statusColor);
        status.setTextSize(TEXT_XS); status.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        row.addView(name); row.addView(status);
        return row;
    }
}