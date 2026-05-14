package com.harsha.kirana;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import static com.harsha.kirana.AppTheme.*;
import static com.harsha.kirana.UiHelper.*;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText etPhone, etPassword;
    private Button btnLogin;
    private TextView tvError;
    private ProgressBar progress;
    private LinearLayout formCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        setContentView(buildUI());
    }

    private ScrollView buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG_PAGE);   // light blue bg
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(this,24), dp(this,60), dp(this,24), dp(this,40));

        // Blue logo box
        LinearLayout logoBox = new LinearLayout(this);
        logoBox.setGravity(Gravity.CENTER);
        logoBox.setBackground(roundedBg(BLUE, 24, this));
        int sz = dp(this, 84);
        LinearLayout.LayoutParams lbP = new LinearLayout.LayoutParams(sz, sz);
        lbP.gravity = Gravity.CENTER_HORIZONTAL;
        lbP.setMargins(0, 0, 0, dp(this, 20));
        logoBox.setLayoutParams(lbP);
        TextView logoTv = new TextView(this);
        logoTv.setText("GK");
        logoTv.setTextColor(0xFFFFFFFF);
        logoTv.setTextSize(30f);
        logoTv.setTypeface(Typeface.DEFAULT_BOLD);
        logoTv.setGravity(Gravity.CENTER);
        logoBox.addView(logoTv);
        root.addView(logoBox);

        // App name — dark on light
        TextView appName = new TextView(this);
        appName.setText("GoKirana");
        appName.setTextColor(TEXT_WHITE);
        appName.setTextSize(28f);
        appName.setTypeface(Typeface.DEFAULT_BOLD);
        appName.setGravity(Gravity.CENTER);
        root.addView(appName);

        View ul = new View(this);
        ul.setBackground(roundedBg(BLUE, 4, this));
        LinearLayout.LayoutParams ulP = new LinearLayout.LayoutParams(dp(this,50), dp(this,3));
        ulP.gravity = Gravity.CENTER_HORIZONTAL;
        ulP.setMargins(0, dp(this,8), 0, dp(this,6));
        ul.setLayoutParams(ulP);
        root.addView(ul);

        TextView sub = new TextView(this);
        sub.setText("Login to your store");
        sub.setTextColor(TEXT_MUTED);
        sub.setTextSize(TEXT_MD);
        sub.setTypeface(Typeface.DEFAULT);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subP.setMargins(0, 0, 0, dp(this,28));
        sub.setLayoutParams(subP);
        root.addView(sub);

        // White form card with shadow-like border
        formCard = new LinearLayout(this);
        formCard.setOrientation(LinearLayout.VERTICAL);
        formCard.setPadding(dp(this,20), dp(this,24), dp(this,20), dp(this,24));
        formCard.setBackground(roundedStroke(BG_CARD, BLACK_BORDER, 18, 1, this));
        formCard.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        formCard.addView(fieldLabel("Phone Number"));
        etPhone = darkField(this, "Enter your phone number", InputType.TYPE_CLASS_PHONE);
        formCard.addView(etPhone);

        formCard.addView(fieldLabel("Password"));
        etPassword = darkField(this, "Enter your password",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        formCard.addView(etPassword);

        tvError = new TextView(this);
        tvError.setTextColor(RED); tvError.setTextSize(TEXT_SM);
        tvError.setTypeface(Typeface.DEFAULT); tvError.setVisibility(View.GONE);
        LinearLayout.LayoutParams errP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        errP.setMargins(0, 0, 0, dp(this,8));
        tvError.setLayoutParams(errP);
        formCard.addView(tvError);

        btnLogin = orangeButton(this, "Login");
        btnLogin.setOnClickListener(v -> { AnimationHelper.pulse(btnLogin); attemptLogin(); });
        formCard.addView(btnLogin);

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams pbP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pbP.gravity = Gravity.CENTER_HORIZONTAL;
        pbP.setMargins(0, dp(this,10), 0, 0);
        progress.setLayoutParams(pbP);
        formCard.addView(progress);
        root.addView(formCard);

        TextView signupLink = new TextView(this);
        signupLink.setText("New to GoKirana? Sign Up");
        signupLink.setTextColor(BLUE);
        signupLink.setTextSize(TEXT_MD);
        signupLink.setTypeface(Typeface.DEFAULT);
        signupLink.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams slP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slP.gravity = Gravity.CENTER_HORIZONTAL;
        slP.setMargins(0, dp(this,20), 0, 0);
        signupLink.setLayoutParams(slP);
        signupLink.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class)); finish();
        });
        root.addView(signupLink);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AnimationHelper.slideUpFadeIn(logoBox,  350, 0);
            AnimationHelper.slideUpFadeIn(formCard, 400, 150);
        }, 100);

        scroll.addView(root);
        return scroll;
    }

    private TextView fieldLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text); tv.setTextColor(TEXT_MUTED);
        tv.setTextSize(TEXT_SM); tv.setTypeface(Typeface.DEFAULT);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, dp(this,6));
        tv.setLayoutParams(p);
        return tv;
    }

    private void attemptLogin() {
        String phone    = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (phone.isEmpty())    { showErr("Enter your phone number"); AnimationHelper.shake(formCard); return; }
        if (password.isEmpty()) { showErr("Enter your password");     AnimationHelper.shake(formCard); return; }

        setLoading(true);
        mAuth.signInWithEmailAndPassword(phone + "@kirana.app", password)
                .addOnSuccessListener(r -> {
                    setLoading(false);
                    startActivity(new Intent(this, MpinActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finishAffinity();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String m = e.getMessage();
                    if (m != null && m.contains("no user record")) showErr("Phone not registered. Sign up first.");
                    else if (m != null && m.contains("password")) showErr("Wrong password. Try again.");
                    else showErr("Login failed. Check your details.");
                    AnimationHelper.shake(formCard);
                });
    }

    private void showErr(String msg) { tvError.setText(msg); tvError.setVisibility(View.VISIBLE); AnimationHelper.fadeIn(tvError, 200); }
    private void setLoading(boolean on) {
        btnLogin.setEnabled(!on); btnLogin.setText(on ? "Logging in..." : "Login");
        progress.setVisibility(on ? View.VISIBLE : View.GONE);
        if (on) tvError.setVisibility(View.GONE);
    }
}