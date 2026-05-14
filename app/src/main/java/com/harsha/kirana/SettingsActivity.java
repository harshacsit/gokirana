package com.harsha.kirana;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.*;
import static com.harsha.kirana.AppTheme.*;
import static com.harsha.kirana.UiHelper.*;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS        = "kirana_prefs";
    private static final String KEY_MPIN     = "user_mpin";
    private static final String KEY_MPIN_SET = "mpin_set";
    private static final String KEY_PHOTO    = "profile_photo_path";

    private FirebaseFirestore db;
    private TextView tvName, tvPhone;
    private ImageView profilePhoto;
    private TextView avatarInitial;
    private LinearLayout avatarContainer;

    private final ActivityResultLauncher<Intent> photoPicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) saveAndShowPhoto(uri);
                }
            });

    private String s(String key) { return LangHelper.t(this, key); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        setContentView(buildUI());
        loadUserData();
        loadSavedPhoto();
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

        TextView title = new TextView(this);
        title.setText(s("settings"));
        title.setTextColor(0xFFFFFFFF);   // white on blue
        title.setTextSize(TEXT_XL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        topRow.addView(accent); topRow.addView(title);
        topBar.addView(topRow);
        root.addView(topBar);

        // ── Scrollable content — light blue ───────────────────────────────
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG_PAGE);   // ← light blue NOT black
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(BG_PAGE);   // ← light blue NOT black
        content.setPadding(dp(this,14), 0, dp(this,14), dp(this,80));

        // ── Profile — white card ───────────────────────────────────────────
        content.addView(sectionLabel(this, s("profile").toUpperCase()));
        LinearLayout profileCard = whiteCard();

        LinearLayout photoRow = new LinearLayout(this);
        photoRow.setOrientation(LinearLayout.HORIZONTAL);
        photoRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams prP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        prP.setMargins(0, 0, 0, dp(this,4));
        photoRow.setLayoutParams(prP);

        avatarContainer = new LinearLayout(this);
        avatarContainer.setGravity(Gravity.CENTER);
        int avSz = dp(this, 72);
        LinearLayout.LayoutParams avP = new LinearLayout.LayoutParams(avSz, avSz);
        avP.setMargins(0, 0, dp(this,16), 0);
        avatarContainer.setLayoutParams(avP);
        avatarContainer.setBackground(roundedBg(BLUE, 36, this));
        avatarContainer.setClickable(true); avatarContainer.setFocusable(true);
        avatarContainer.setOnClickListener(v -> openPhotoPicker());

        avatarInitial = new TextView(this);
        avatarInitial.setText("G"); avatarInitial.setTextColor(0xFFFFFFFF);
        avatarInitial.setTextSize(28f); avatarInitial.setTypeface(Typeface.DEFAULT_BOLD);
        avatarInitial.setGravity(Gravity.CENTER);

        profilePhoto = new ImageView(this);
        profilePhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        profilePhoto.setLayoutParams(new LinearLayout.LayoutParams(avSz, avSz));
        profilePhoto.setVisibility(View.GONE);
        profilePhoto.setClipToOutline(true);
        profilePhoto.setBackground(roundedBg(BLUE, 36, this));

        avatarContainer.addView(avatarInitial); avatarContainer.addView(profilePhoto);

        LinearLayout nameCol = new LinearLayout(this);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        nameCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        tvName = new TextView(this);
        tvName.setText(s("loading"));
        tvName.setTextColor(TEXT_WHITE); tvName.setTextSize(TEXT_LG);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);

        tvPhone = new TextView(this);
        tvPhone.setText("...");
        tvPhone.setTextColor(TEXT_MUTED); tvPhone.setTextSize(TEXT_MD);
        tvPhone.setTypeface(Typeface.DEFAULT);
        LinearLayout.LayoutParams phP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        phP.setMargins(0, dp(this,4), 0, dp(this,8));
        tvPhone.setLayoutParams(phP);

        TextView tapPhoto = new TextView(this);
        tapPhoto.setText(s("tap_photo"));
        tapPhoto.setTextColor(BLUE); tapPhoto.setTextSize(TEXT_XS);
        tapPhoto.setTypeface(Typeface.DEFAULT); tapPhoto.setClickable(true);
        tapPhoto.setOnClickListener(v -> openPhotoPicker());

        nameCol.addView(tvName); nameCol.addView(tvPhone); nameCol.addView(tapPhoto);
        photoRow.addView(avatarContainer); photoRow.addView(nameCol);
        profileCard.addView(photoRow);
        content.addView(profileCard);

        // ── Language — white card ──────────────────────────────────────────
        content.addView(sectionLabel(this, s("language").toUpperCase()));
        LinearLayout langCard = whiteCard();

        LinearLayout langRow = new LinearLayout(this);
        langRow.setOrientation(LinearLayout.HORIZONTAL);
        langRow.setGravity(Gravity.CENTER_VERTICAL);
        langRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout langTextCol = new LinearLayout(this);
        langTextCol.setOrientation(LinearLayout.VERTICAL);
        langTextCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView langLabel = new TextView(this);
        langLabel.setText(s("language"));
        langLabel.setTextColor(TEXT_WHITE); langLabel.setTextSize(TEXT_MD);
        langLabel.setTypeface(Typeface.DEFAULT_BOLD);

        TextView langCurrent = new TextView(this);
        langCurrent.setText(LangHelper.currentLangName(this));
        langCurrent.setTextColor(TEXT_MUTED); langCurrent.setTextSize(TEXT_SM);
        LinearLayout.LayoutParams lcP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lcP.setMargins(0, dp(this,2), 0, 0);
        langCurrent.setLayoutParams(lcP);

        langTextCol.addView(langLabel); langTextCol.addView(langCurrent);

        LinearLayout toggleBtn = new LinearLayout(this);
        toggleBtn.setGravity(Gravity.CENTER);
        toggleBtn.setPadding(dp(this,16), dp(this,10), dp(this,16), dp(this,10));
        toggleBtn.setBackground(roundedBg(BLUE_DIM, 20, this));
        toggleBtn.setClickable(true); toggleBtn.setFocusable(true);
        toggleBtn.setOnClickListener(v -> {
            LangHelper.toggle(this);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                setContentView(buildUI());
                loadUserData(); loadSavedPhoto();
            }, 150);
        });
        TextView toggleTv = new TextView(this);
        toggleTv.setText(LangHelper.isTelugu(this) ? "Switch to English" : "తెలుగుకి మార్చు");
        toggleTv.setTextColor(BLUE); toggleTv.setTextSize(TEXT_SM);
        toggleTv.setTypeface(Typeface.DEFAULT_BOLD);
        toggleBtn.addView(toggleTv);

        langRow.addView(langTextCol); langRow.addView(toggleBtn);
        langCard.addView(langRow);
        content.addView(langCard);

        // ── Security — white card ──────────────────────────────────────────
        content.addView(sectionLabel(this, s("security").toUpperCase()));
        LinearLayout secCard = whiteCard();

        boolean mpinSet = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_MPIN_SET, false);
        TextView mpinStatus = new TextView(this);
        mpinStatus.setText(mpinSet ? "✅  " + s("mpin_active") : "⚠️  MPIN not set");
        mpinStatus.setTextColor(mpinSet ? GREEN : AMBER);
        mpinStatus.setTextSize(TEXT_MD); mpinStatus.setTypeface(Typeface.DEFAULT);
        LinearLayout.LayoutParams msP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        msP.setMargins(0, 0, 0, dp(this,12));
        mpinStatus.setLayoutParams(msP);
        secCard.addView(mpinStatus);
        secCard.addView(divider());
        secCard.addView(buildAction(s("change_mpin"), "Update your 4-digit PIN", BLUE, () -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .remove(KEY_MPIN).putBoolean(KEY_MPIN_SET, false).apply();
            startActivity(new Intent(this, MpinActivity.class));
        }));
        content.addView(secCard);

        // ── About — white card ─────────────────────────────────────────────
        content.addView(sectionLabel(this, s("about").toUpperCase()));
        LinearLayout aboutCard = whiteCard();
        aboutCard.addView(infoRow("App", "GoKirana"));
        aboutCard.addView(divider());
        aboutCard.addView(infoRow("Version", "2.0"));
        aboutCard.addView(divider());
        aboutCard.addView(infoRow("Backend", "Firebase Firestore"));
        aboutCard.addView(divider());
        aboutCard.addView(infoRow("Alerts", "Daily 9 AM + WhatsApp"));
        content.addView(aboutCard);

        // ── Feedback ───────────────────────────────────────────────────────
        content.addView(sectionLabel(this, LangHelper.isTelugu(this) ? "అభిప్రాయం" : "FEEDBACK"));
        LinearLayout feedbackCard = whiteCard();
        feedbackCard.addView(buildAction(
                LangHelper.isTelugu(this) ? "📝  అభిప్రాయం ఇవ్వండి" : "📝  Send Feedback",
                LangHelper.isTelugu(this) ? "రేటింగ్, సూచన, బగ్ రిపోర్ట్ పంపండి" : "Rate app, suggest features, report bugs",
                BLUE,
                () -> startActivity(new Intent(this, FeedbackActivity.class))
        ));
        content.addView(feedbackCard);

        // ── Logout ─────────────────────────────────────────────────────────
        content.addView(sectionLabel(this, "ACCOUNT"));
        Button btnLogout = new Button(this);
        btnLogout.setText(s("logout"));
        btnLogout.setTextColor(RED); btnLogout.setTextSize(TEXT_MD);
        btnLogout.setTypeface(Typeface.DEFAULT_BOLD);
        btnLogout.setBackground(roundedStroke(BG_CARD, RED, 14, 1, this));   // white bg red border
        btnLogout.setAllCaps(false);
        LinearLayout.LayoutParams loP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(this,56));
        loP.setMargins(0, 0, 0, dp(this,10));
        btnLogout.setLayoutParams(loP);
        btnLogout.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(s("logout_confirm"))
                .setMessage(s("logout_msg"))
                .setPositiveButton(s("logout"), (d, w) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .remove(KEY_MPIN).remove(KEY_MPIN_SET)
                            .remove("fail_count").remove("lock_time").apply();
                    FirebaseAuth.getInstance().signOut();
                    AlertSession.reset();   // clear session alerts on logout
                    VoiceAlertHelper.shutdown();
                    startActivity(new Intent(this, LoginActivity.class));
                    finishAffinity();
                })
                .setNegativeButton(s("cancel"), null).show());
        content.addView(btnLogout);

        scroll.addView(content);
        root.addView(scroll);

        String[] labels = {"Home", s("stock"), s("analytics"), s("settings")};
        String[] icons  = {"🏠","📦","📊","⚙️"};
        Runnable[] clicks = {
                () -> { startActivity(new Intent(this, HomeActivity.class)); finish(); },
                () -> { startActivity(new Intent(this, DashboardActivity.class)); finish(); },
                () -> { startActivity(new Intent(this, AnalyticsActivity.class)); finish(); },
                null
        };
        root.addView(bottomNav(this, labels, icons, 3, clicks));
        return root;
    }

    // ── White card ─────────────────────────────────────────────────────────
    private LinearLayout whiteCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(this,16), dp(this,16), dp(this,16), dp(this,16));
        card.setBackground(roundedStroke(BG_CARD, BLACK_BORDER, 14, 1, this));   // WHITE
        card.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private View divider() {
        View div = new View(this);
        div.setBackgroundColor(BLACK_BORDER);   // light border
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        p.setMargins(0, dp(this,10), 0, dp(this,10));
        div.setLayoutParams(p);
        return div;
    }

    private LinearLayout infoRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(this,4), 0, dp(this,4));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView lbl = new TextView(this);
        lbl.setText(label); lbl.setTextColor(TEXT_MUTED);
        lbl.setTextSize(TEXT_MD); lbl.setTypeface(Typeface.DEFAULT);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(dp(this,110), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView val = new TextView(this);
        val.setText(value); val.setTextColor(TEXT_WHITE);
        val.setTextSize(TEXT_MD); val.setTypeface(Typeface.DEFAULT_BOLD);
        val.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(lbl); row.addView(val);
        return row;
    }

    private LinearLayout buildAction(String title, String desc, int color, Runnable onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(this,6), 0, dp(this,6));
        row.setClickable(true); row.setFocusable(true);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setOnClickListener(v -> onClick.run());
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView t = new TextView(this);
        t.setText(title); t.setTextColor(color);
        t.setTextSize(TEXT_MD); t.setTypeface(Typeface.DEFAULT_BOLD);
        TextView d = new TextView(this);
        d.setText(desc); d.setTextColor(TEXT_MUTED);
        d.setTextSize(TEXT_SM); d.setTypeface(Typeface.DEFAULT);
        col.addView(t); col.addView(d);
        TextView arrow = new TextView(this);
        arrow.setText("›"); arrow.setTextColor(TEXT_MUTED); arrow.setTextSize(22f);
        row.addView(col); row.addView(arrow);
        return row;
    }

    private void openPhotoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        photoPicker.launch(intent);
    }

    private void saveAndShowPhoto(Uri uri) {
        try {
            Bitmap original = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            Bitmap scaled = Bitmap.createScaledBitmap(original, 300, 300, true);
            File file = new File(getFilesDir(), "profile_photo.jpg");
            FileOutputStream fos = new FileOutputStream(file);
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, fos); fos.close();
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_PHOTO, file.getAbsolutePath()).apply();
            showPhoto(scaled);
            Toast.makeText(this, s("photo_updated"), Toast.LENGTH_SHORT).show();
        } catch (Exception e) { Toast.makeText(this, "Could not set photo", Toast.LENGTH_SHORT).show(); }
    }

    private void loadSavedPhoto() {
        String path = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_PHOTO, null);
        if (path != null) {
            File file = new File(path);
            if (file.exists()) { Bitmap bmp = BitmapFactory.decodeFile(path); if (bmp != null) showPhoto(bmp); }
        }
    }

    private void showPhoto(Bitmap bmp) {
        profilePhoto.setImageBitmap(bmp);
        profilePhoto.setVisibility(View.VISIBLE);
        avatarInitial.setVisibility(View.GONE);
        avatarContainer.setBackground(null);
    }

    private void loadUserData() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    String name = doc.getString("name");
                    String phone = doc.getString("phone");
                    if (name  != null) { tvName.setText(name); avatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase()); }
                    if (phone != null) tvPhone.setText(phone);
                });
    }
}