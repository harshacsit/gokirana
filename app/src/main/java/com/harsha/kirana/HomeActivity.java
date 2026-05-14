package com.harsha.kirana;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.File;
import java.util.List;
import static com.harsha.kirana.AppTheme.*;
import static com.harsha.kirana.UiHelper.*;

public class HomeActivity extends AppCompatActivity {

    private static final String PREFS     = "kirana_prefs";
    private static final String KEY_PHOTO = "profile_photo_path";

    private DashboardViewModel viewModel;
    private FirebaseFirestore db;
    private TextView tvName, tvGreeting, tvTotalVal, tvLowVal, tvExpVal;
    private ImageView avatarPhoto;
    private TextView avatarInitial;
    private LinearLayout avatarContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db        = FirebaseFirestore.getInstance();
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        viewModel.setContext(this);
        setContentView(buildUI());
        loadUserData();
        loadProfilePhoto();

        viewModel.getProductsLiveData().observe(this, products -> {
            tvTotalVal.setText(String.valueOf(viewModel.totalItems()));
            tvLowVal.setText(String.valueOf(viewModel.getLowStockItems().size()));
            tvExpVal.setText(String.valueOf(viewModel.getExpiringItems().size()));
        });

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            List<Product> exp = viewModel.getExpiringItems();
            List<Product> low = viewModel.getLowStockItems();
            if (!exp.isEmpty()) NotificationHelper.sendExpiryNotification(this, exp);
            if (!low.isEmpty()) NotificationHelper.sendLowStockNotification(this, low);
            if (!exp.isEmpty() || !low.isEmpty()) viewModel. sendWhatsAppAlert();
        }, 3000);
    }

    @Override
    protected void onResume() { super.onResume(); loadProfilePhoto(); }

    private LinearLayout buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG_PAGE);   // light blue page

        // ── Blue top bar ──────────────────────────────────────────────────
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.VERTICAL);
        topBar.setBackgroundColor(BG_TOPBAR);  // solid blue
        topBar.setPadding(dp(this,18), dp(this,48), dp(this,18), dp(this,20));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        tvGreeting = new TextView(this);
        tvGreeting.setText("Good day");
        tvGreeting.setTextColor(0xCCFFFFFF);    // semi-white on blue
        tvGreeting.setTextSize(TEXT_SM);
        tvGreeting.setTypeface(Typeface.DEFAULT);

        tvName = new TextView(this);
        tvName.setText("GoKirana");
        tvName.setTextColor(0xFFFFFFFF);         // white on blue
        tvName.setTextSize(TEXT_XL);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);

        textCol.addView(tvGreeting);
        textCol.addView(tvName);

        // White avatar circle on blue bar
        avatarContainer = new LinearLayout(this);
        avatarContainer.setGravity(Gravity.CENTER);
        int avSz = dp(this, 50);
        LinearLayout.LayoutParams avP = new LinearLayout.LayoutParams(avSz, avSz);
        avatarContainer.setLayoutParams(avP);
        avatarContainer.setBackground(roundedBg(0xFFFFFFFF, 25, this));
        avatarContainer.setClickable(true); avatarContainer.setFocusable(true);
        avatarContainer.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        avatarInitial = new TextView(this);
        avatarInitial.setText("G");
        avatarInitial.setTextColor(BLUE);       // blue text on white circle
        avatarInitial.setTextSize(18f);
        avatarInitial.setTypeface(Typeface.DEFAULT_BOLD);
        avatarInitial.setGravity(Gravity.CENTER);

        avatarPhoto = new ImageView(this);
        avatarPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatarPhoto.setLayoutParams(new LinearLayout.LayoutParams(avSz, avSz));
        avatarPhoto.setVisibility(View.GONE);
        avatarPhoto.setClipToOutline(true);
        avatarPhoto.setBackground(roundedBg(0xFFFFFFFF, 25, this));

        avatarContainer.addView(avatarInitial);
        avatarContainer.addView(avatarPhoto);

        topRow.addView(textCol);
        topRow.addView(avatarContainer);
        topBar.addView(topRow);

        // White stats chips on blue bar
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams srP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        srP.setMargins(0, dp(this,16), 0, 0);
        statsRow.setLayoutParams(srP);
        tvTotalVal = addStatChip(statsRow, "0", "Products", 0xFFFFFFFF, 0x33FFFFFF, true);
        tvLowVal   = addStatChip(statsRow, "0", "Low Stock", 0xFFFFECB3, 0x33FFB300, false);
        tvExpVal   = addStatChip(statsRow, "0", "Expiring",  0xFFFFCDD2, 0x33E53935, false);
        topBar.addView(statsRow);
        root.addView(topBar);

        // Scrollable content — light blue bg
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG_PAGE);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(BG_PAGE);

        content.addView(sectionLabel(this, "QUICK ACTIONS"));

        LinearLayout row1 = actionRow();
        row1.addView(actionCard("📦", "Stock", "Add & track products", BLUE,
                () -> startActivity(new Intent(this, DashboardActivity.class))));
        row1.addView(actionCard("📊", "Analytics", "Charts & insights", GREEN,
                () -> startActivity(new Intent(this, AnalyticsActivity.class))));
        content.addView(row1);

        LinearLayout row2 = actionRow();
        row2.addView(actionCard("⚠️", "Low Stock", "Items to restock", AMBER,
                () -> startActivity(new Intent(this, DashboardActivity.class))));
        row2.addView(actionCard("⚙️", "Settings", "Account & MPIN", TEXT_MUTED,
                () -> startActivity(new Intent(this, SettingsActivity.class))));
        content.addView(row2);

        content.addView(sectionLabel(this, "ALERTS"));
        content.addView(buildAlertCard());

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(this,20)));
        content.addView(spacer);
        scroll.addView(content);
        root.addView(scroll);

        String[] labels = {"Home","Stock","Analytics","Settings"};
        String[] icons  = {"🏠","📦","📊","⚙️"};
        Runnable[] clicks = {
                null,
                () -> startActivity(new Intent(this, DashboardActivity.class)),
                () -> startActivity(new Intent(this, AnalyticsActivity.class)),
                () -> startActivity(new Intent(this, SettingsActivity.class))
        };
        root.addView(bottomNav(this, labels, icons, 0, clicks));
        return root;
    }

    private TextView addStatChip(LinearLayout parent, String value, String label,
                                 int valueColor, int bgColor, boolean first) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(this,12), dp(this,10), dp(this,12), dp(this,10));
        chip.setBackground(roundedBg(bgColor, 12, this));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (!first) cp.setMargins(dp(this,8), 0, 0, 0);
        chip.setLayoutParams(cp);

        TextView val = new TextView(this);
        val.setText(value); val.setTextColor(valueColor);
        val.setTextSize(24f); val.setTypeface(Typeface.DEFAULT_BOLD);
        val.setGravity(Gravity.CENTER);

        TextView lbl = new TextView(this);
        lbl.setText(label); lbl.setTextColor(0xAAFFFFFF);
        lbl.setTextSize(TEXT_XS); lbl.setTypeface(Typeface.DEFAULT);
        lbl.setGravity(Gravity.CENTER);

        chip.addView(val); chip.addView(lbl);
        parent.addView(chip);
        return val;
    }

    private LinearLayout actionRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(dp(this,14), 0, dp(this,14), dp(this,10));
        row.setLayoutParams(p);
        return row;
    }

    private LinearLayout actionCard(String emoji, String title, String desc,
                                    int color, Runnable onClick) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(this,16), dp(this,16), dp(this,16), dp(this,16));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(0, 0, dp(this,8), 0);
        card.setLayoutParams(p);
        card.setBackground(roundedStroke(BG_CARD, BLACK_BORDER, 14, 1, this));
        card.setClickable(true); card.setFocusable(true);
        card.setOnClickListener(v -> { AnimationHelper.pulse(card); new Handler(Looper.getMainLooper()).postDelayed(onClick::run, 100); });

        View dot = new View(this);
        dot.setBackground(ovalBg(color));
        LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(dp(this,8), dp(this,8));
        dp2.setMargins(0, 0, 0, dp(this,10));
        dot.setLayoutParams(dp2);

        TextView emojiTv = new TextView(this);
        emojiTv.setText(emoji); emojiTv.setTextSize(30f);

        TextView titleTv = new TextView(this);
        titleTv.setText(title); titleTv.setTextColor(TEXT_WHITE);
        titleTv.setTextSize(TEXT_LG); titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tP.setMargins(0, dp(this,8), 0, dp(this,4));
        titleTv.setLayoutParams(tP);

        TextView descTv = new TextView(this);
        descTv.setText(desc); descTv.setTextColor(TEXT_MUTED);
        descTv.setTextSize(TEXT_SM); descTv.setTypeface(Typeface.DEFAULT);

        card.addView(dot); card.addView(emojiTv); card.addView(titleTv); card.addView(descTv);
        return card;
    }

    private LinearLayout buildAlertCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(this,16), dp(this,14), dp(this,16), dp(this,14));
        card.setBackground(roundedStroke(BLUE_DIM, BLUE, 14, 1, this));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(dp(this,14), 0, dp(this,14), 0);
        card.setLayoutParams(p);

        String[] lines = {
                "Daily alerts at 9 AM automatically",
               " WhatsApp message. They must have joined the sandbox ",
                "once send join <code> to +14155238886 from their WhatsApp",
                "Expiry alert 3 days before and on date",
                "Low stock alert when quantity is low",
                "WhatsApp message in your language"
        };
        for (int i = 0; i < lines.length; i++) {
            TextView tv = new TextView(this);
            tv.setText((i == 0 ? "🔔  " : "     ") + lines[i]);
            tv.setTextColor(i == 0 ? BLUE : TEXT_MUTED);
            tv.setTextSize(TEXT_MD);
            tv.setTypeface(i == 0 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            if (i > 0) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, dp(this,4), 0, 0);
                tv.setLayoutParams(lp);
            }
            card.addView(tv);
        }
        return card;
    }

    private void loadProfilePhoto() {
        String path = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_PHOTO, null);
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(path);
                if (bmp != null) {
                    avatarPhoto.setImageBitmap(bmp);
                    avatarPhoto.setVisibility(View.VISIBLE);
                    avatarInitial.setVisibility(View.GONE);
                    avatarContainer.setBackground(null);
                }
            }
        }
    }

    private void loadUserData() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        tvGreeting.setText(hour < 12 ? "Good morning ☀️" : hour < 17 ? "Good afternoon 👋" : "Good evening 🌙");

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            tvName.setText(name + "'s Store");
                            avatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                            AnimationHelper.fadeIn(tvName, 300);
                        }
                    }
                });
    }
}