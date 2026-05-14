package com.harsha.kirana;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import static com.harsha.kirana.AppTheme.*;
import static com.harsha.kirana.UiHelper.*;

public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText etName, etPhone, etPassword, etConfirm;
    private Button btnSignup;
    private TextView tvError;
    private ProgressBar progress;
    private LinearLayout formCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();
        setContentView(buildUI());
    }

    private ScrollView buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG_PAGE);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(this,24), dp(this,60), dp(this,24), dp(this,40));

        LinearLayout logoBox = new LinearLayout(this);
        logoBox.setGravity(Gravity.CENTER);
        logoBox.setBackground(roundedBg(BLUE, 24, this));
        int sz = dp(this, 80);
        LinearLayout.LayoutParams lbP = new LinearLayout.LayoutParams(sz, sz);
        lbP.gravity = Gravity.CENTER_HORIZONTAL;
        lbP.setMargins(0, 0, 0, dp(this, 16));
        logoBox.setLayoutParams(lbP);
        TextView logo = new TextView(this);
        logo.setText("GK"); logo.setTextColor(0xFFFFFFFF);
        logo.setTextSize(28f); logo.setTypeface(Typeface.DEFAULT_BOLD);
        logo.setGravity(Gravity.CENTER);
        logoBox.addView(logo);
        root.addView(logoBox);

        TextView title = new TextView(this);
        title.setText("Create Account");
        title.setTextColor(TEXT_WHITE); title.setTextSize(24f);
        title.setTypeface(Typeface.DEFAULT_BOLD); title.setGravity(Gravity.CENTER);
        root.addView(title);

        View ul = new View(this);
        ul.setBackground(roundedBg(BLUE, 4, this));
        LinearLayout.LayoutParams ulP = new LinearLayout.LayoutParams(dp(this,50), dp(this,3));
        ulP.gravity = Gravity.CENTER_HORIZONTAL;
        ulP.setMargins(0, dp(this,8), 0, dp(this,6));
        ul.setLayoutParams(ulP);
        root.addView(ul);

        TextView sub = new TextView(this);
        sub.setText("Join GoKirana today");
        sub.setTextColor(TEXT_MUTED); sub.setTextSize(TEXT_MD);
        sub.setTypeface(Typeface.DEFAULT); sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subP.setMargins(0, 0, 0, dp(this,24));
        sub.setLayoutParams(subP);
        root.addView(sub);

        formCard = new LinearLayout(this);
        formCard.setOrientation(LinearLayout.VERTICAL);
        formCard.setPadding(dp(this,20), dp(this,24), dp(this,20), dp(this,24));
        formCard.setBackground(roundedStroke(BG_CARD, BLACK_BORDER, 18, 1, this));
        formCard.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        formCard.addView(fl("Full Name"));
        etName = darkField(this, "Your full name",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        formCard.addView(etName);

        formCard.addView(fl("Phone Number"));
        etPhone = darkField(this, "10-digit phone number", InputType.TYPE_CLASS_PHONE);
        formCard.addView(etPhone);

        formCard.addView(fl("Password"));
        etPassword = darkField(this, "Min 6 characters",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        formCard.addView(etPassword);

        formCard.addView(fl("Confirm Password"));
        etConfirm = darkField(this, "Re-enter password",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        formCard.addView(etConfirm);

        tvError = new TextView(this);
        tvError.setTextColor(RED); tvError.setTextSize(TEXT_SM);
        tvError.setVisibility(View.GONE);
        LinearLayout.LayoutParams errP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        errP.setMargins(0, 0, 0, dp(this,8));
        tvError.setLayoutParams(errP);
        formCard.addView(tvError);

        btnSignup = orangeButton(this, "Create Account");
        btnSignup.setOnClickListener(v -> { AnimationHelper.pulse(btnSignup); attemptSignup(); });
        formCard.addView(btnSignup);

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams pbP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pbP.gravity = Gravity.CENTER_HORIZONTAL;
        pbP.setMargins(0, dp(this,10), 0, 0);
        progress.setLayoutParams(pbP);
        formCard.addView(progress);
        root.addView(formCard);

        TextView loginLink = new TextView(this);
        loginLink.setText("Already have an account? Login");
        loginLink.setTextColor(BLUE); loginLink.setTextSize(TEXT_MD);
        loginLink.setTypeface(Typeface.DEFAULT); loginLink.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams llP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llP.gravity = Gravity.CENTER_HORIZONTAL;
        llP.setMargins(0, dp(this,20), 0, 0);
        loginLink.setLayoutParams(llP);
        loginLink.setOnClickListener(v -> { startActivity(new Intent(this, LoginActivity.class)); finish(); });
        root.addView(loginLink);

        scroll.addView(root);
        return scroll;
    }

    private TextView fl(String t) {
        TextView tv = new TextView(this);
        tv.setText(t); tv.setTextColor(TEXT_MUTED);
        tv.setTextSize(TEXT_SM); tv.setTypeface(Typeface.DEFAULT);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, dp(this,6));
        tv.setLayoutParams(p);
        return tv;
    }

    private void attemptSignup() {
        String name     = etName.getText().toString().trim();
        String phone    = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm  = etConfirm.getText().toString().trim();
        if (name.isEmpty())             { showErr("Enter your full name");      AnimationHelper.shake(formCard); return; }
        // Strict: exactly 10 digits, starts with 6-9 (Indian mobile numbers)
        if (!phone.matches("[6-9][0-9]{9}")) { showErr("Enter valid 10-digit number (starts with 6,7,8 or 9)"); AnimationHelper.shake(formCard); return; }
        if (password.length() < 6)      { showErr("Password min 6 characters");AnimationHelper.shake(formCard); return; }
        if (!password.equals(confirm))  { showErr("Passwords do not match");   AnimationHelper.shake(formCard); return; }

        setLoading(true);
        mAuth.createUserWithEmailAndPassword(phone + "@kirana.app", password)
                .addOnSuccessListener(r -> saveUser(r.getUser().getUid(), name, phone))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String m = e.getMessage();
                    showErr(m != null && m.contains("already in use")
                            ? "Phone already registered. Login instead." : "Signup failed: " + m);
                    AnimationHelper.shake(formCard);
                });
    }

    private void saveUser(String uid, String name, String phone) {
        Map<String,Object> u = new HashMap<>();
        u.put("name", name); u.put("phone", phone); u.put("createdAt", System.currentTimeMillis());
        db.collection("users").document(uid).set(u)
                .addOnSuccessListener(x -> {
                    setLoading(false);
                    Toast.makeText(this, "Welcome, " + name + "!", Toast.LENGTH_SHORT).show();
                    // Send WhatsApp welcome message via Twilio
                    WhatsAppHelper.sendWelcomeMessage(phone, name);
                    startActivity(new Intent(this, MpinActivity.class));
                    finishAffinity();
                })
                .addOnFailureListener(e -> { setLoading(false); startActivity(new Intent(this, LoginActivity.class)); finish(); });
    }

    private void showErr(String msg) { tvError.setText(msg); tvError.setVisibility(View.VISIBLE); }
    private void setLoading(boolean on) {
        btnSignup.setEnabled(!on); btnSignup.setText(on ? "Creating account..." : "Create Account");
        progress.setVisibility(on ? View.VISIBLE : View.GONE);
        if (on) tvError.setVisibility(View.GONE);
    }
}