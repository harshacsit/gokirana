package com.harsha.kirana;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import static com.harsha.kirana.AppTheme.*;
import static com.harsha.kirana.UiHelper.*;

public class MpinActivity extends AppCompatActivity {

    private static final String PREFS          = "kirana_prefs";
    private static final String KEY_MPIN       = "user_mpin";
    private static final String KEY_MPIN_SET   = "mpin_set";
    private static final String KEY_FAIL_COUNT = "fail_count";
    private static final String KEY_LOCK_TIME  = "lock_time";
    private static final int    MAX_ATTEMPTS   = 3;
    private static final long   LOCK_DURATION  = 30_000L;

    private static final int MODE_SET    = 0;
    private static final int MODE_VERIFY = 1;

    private int     mode;
    private String  enteredPin  = "";
    private String  firstPin    = "";
    private boolean isConfirming = false;

    private View[]       dots = new View[4];
    private TextView     tvTitle, tvSubtitle, tvError, tvAttempts, tvLockMsg;
    private LinearLayout dotRow, keypad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        mode = prefs.getBoolean(KEY_MPIN_SET, false) ? MODE_VERIFY : MODE_SET;
        setContentView(buildUI());
        checkLockout();
    }

    private LinearLayout buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG_PAGE);   // light blue bg
        root.setGravity(Gravity.CENTER);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(this,28), dp(this,60), dp(this,28), dp(this,40));
        content.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Blue logo
        LinearLayout logoBox = new LinearLayout(this);
        logoBox.setGravity(Gravity.CENTER);
        logoBox.setBackground(roundedBg(BLUE, 20, this));
        int sz = dp(this, 72);
        LinearLayout.LayoutParams lbP = new LinearLayout.LayoutParams(sz, sz);
        lbP.gravity = Gravity.CENTER_HORIZONTAL;
        lbP.setMargins(0, 0, 0, dp(this, 24));
        logoBox.setLayoutParams(lbP);
        TextView logoTv = new TextView(this);
        logoTv.setText("GK"); logoTv.setTextColor(0xFFFFFFFF);
        logoTv.setTextSize(28f); logoTv.setTypeface(Typeface.DEFAULT_BOLD);
        logoTv.setGravity(Gravity.CENTER);
        logoBox.addView(logoTv);
        content.addView(logoBox);

        tvTitle = new TextView(this);
        tvTitle.setText(mode == MODE_SET ? "Set your MPIN" : "Enter MPIN");
        tvTitle.setTextColor(TEXT_WHITE); tvTitle.setTextSize(24f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD); tvTitle.setGravity(Gravity.CENTER);
        content.addView(tvTitle);

        tvSubtitle = new TextView(this);
        tvSubtitle.setText(mode == MODE_SET ? "Choose a 4-digit MPIN" : "Welcome back!");
        tvSubtitle.setTextColor(TEXT_MUTED); tvSubtitle.setTextSize(TEXT_MD);
        tvSubtitle.setTypeface(Typeface.DEFAULT); tvSubtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subP.setMargins(0, dp(this,6), 0, dp(this,32));
        tvSubtitle.setLayoutParams(subP);
        content.addView(tvSubtitle);

        // 4 MPIN dots
        dotRow = new LinearLayout(this);
        dotRow.setOrientation(LinearLayout.HORIZONTAL);
        dotRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams drP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        drP.setMargins(0, 0, 0, dp(this,10));
        dotRow.setLayoutParams(drP);

        for (int i = 0; i < 4; i++) {
            View dot = new View(this);
            int dotSz = dp(this, 18);
            LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(dotSz, dotSz);
            dp2.setMargins(dp(this,10), 0, dp(this,10), 0);
            dot.setLayoutParams(dp2);
            dot.setBackground(roundedStroke(BG_CARD, BLACK_BORDER, 9, 2, this));
            dots[i] = dot;
            dotRow.addView(dot);
        }
        content.addView(dotRow);

        tvError = new TextView(this);
        tvError.setTextColor(RED); tvError.setTextSize(TEXT_SM);
        tvError.setGravity(Gravity.CENTER); tvError.setVisibility(View.GONE);
        LinearLayout.LayoutParams errP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        errP.setMargins(0, dp(this,4), 0, 0);
        tvError.setLayoutParams(errP);
        content.addView(tvError);

        tvAttempts = new TextView(this);
        tvAttempts.setTextColor(AMBER); tvAttempts.setTextSize(TEXT_SM);
        tvAttempts.setGravity(Gravity.CENTER); tvAttempts.setVisibility(View.GONE);
        content.addView(tvAttempts);

        tvLockMsg = new TextView(this);
        tvLockMsg.setTextColor(RED); tvLockMsg.setTextSize(TEXT_MD);
        tvLockMsg.setTypeface(Typeface.DEFAULT_BOLD);
        tvLockMsg.setGravity(Gravity.CENTER); tvLockMsg.setVisibility(View.GONE);
        content.addView(tvLockMsg);

        // Keypad
        keypad = buildKeypad();
        LinearLayout.LayoutParams kP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        kP.setMargins(0, dp(this,28), 0, 0);
        keypad.setLayoutParams(kP);
        content.addView(keypad);

        if (mode == MODE_VERIFY) {
            TextView logout = new TextView(this);
            logout.setText("Not you? Logout");
            logout.setTextColor(TEXT_MUTED); logout.setTextSize(TEXT_SM);
            logout.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams loP = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            loP.gravity = Gravity.CENTER_HORIZONTAL;
            loP.setMargins(0, dp(this,20), 0, 0);
            logout.setLayoutParams(loP);
            logout.setClickable(true); logout.setFocusable(true);
            logout.setOnClickListener(v -> {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .remove(KEY_MPIN).remove(KEY_MPIN_SET)
                        .remove(KEY_FAIL_COUNT).remove(KEY_LOCK_TIME).apply();
                startActivity(new Intent(this, LoginActivity.class));
                finishAffinity();
            });
            content.addView(logout);
        }

        root.addView(content);
        return root;
    }

    private LinearLayout buildKeypad() {
        LinearLayout kp = new LinearLayout(this);
        kp.setOrientation(LinearLayout.VERTICAL);
        kp.setGravity(Gravity.CENTER);

        String[][] rows = {{"1","2","3"},{"4","5","6"},{"7","8","9"},{"","0","⌫"}};
        for (String[] row : rows) {
            LinearLayout kr = new LinearLayout(this);
            kr.setOrientation(LinearLayout.HORIZONTAL);
            kr.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rp.setMargins(0, 0, 0, dp(this,10));
            kr.setLayoutParams(rp);

            for (String key : row) {
                LinearLayout btn = buildKey(key);
                kr.addView(btn);
            }
            kp.addView(kr);
        }
        return kp;
    }

    private LinearLayout buildKey(String label) {
        LinearLayout btn = new LinearLayout(this);
        btn.setGravity(Gravity.CENTER);
        int keySz = dp(this, 72);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(keySz, keySz);
        p.setMargins(dp(this,8), 0, dp(this,8), 0);
        btn.setLayoutParams(p);

        if (label.isEmpty()) return btn;

        // White keys with blue border — light theme
        if (label.equals("⌫")) {
            btn.setBackground(roundedBg(BG_FIELD, 36, this));
        } else {
            btn.setBackground(roundedStroke(BG_CARD, BLACK_BORDER, 36, 1, this));
        }
        btn.setClickable(true); btn.setFocusable(true);

        TextView keyTv = new TextView(this);
        keyTv.setText(label);
        keyTv.setTextColor(label.equals("⌫") ? TEXT_MUTED : TEXT_WHITE);
        keyTv.setTextSize(22f); keyTv.setTypeface(Typeface.DEFAULT_BOLD);
        keyTv.setGravity(Gravity.CENTER);
        btn.addView(keyTv);

        btn.setOnClickListener(v -> {
            AnimationHelper.pulse(btn);
            if (label.equals("⌫")) onBackspace();
            else                   onDigit(label);
        });
        return btn;
    }

    private void onDigit(String digit) {
        if (enteredPin.length() >= 4) return;
        enteredPin += digit;
        updateDots();
        if (enteredPin.length() == 4)
            new Handler(Looper.getMainLooper()).postDelayed(this::processPin, 200);
    }

    private void onBackspace() {
        if (enteredPin.isEmpty()) return;
        enteredPin = enteredPin.substring(0, enteredPin.length() - 1);
        updateDots();
    }

    private void updateDots() {
        for (int i = 0; i < 4; i++) {
            if (i < enteredPin.length()) {
                dots[i].setBackground(roundedBg(BLUE, 9, this));
                AnimationHelper.bounceIn(dots[i]);
            } else {
                dots[i].setBackground(roundedStroke(BG_CARD, BLACK_BORDER, 9, 2, this));
                AnimationHelper.shrinkOut(dots[i]);
            }
        }
    }

    private void processPin() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (mode == MODE_SET) {
            if (!isConfirming) {
                firstPin = enteredPin; enteredPin = ""; isConfirming = true;
                updateDots();
                tvTitle.setText("Confirm MPIN");
                tvSubtitle.setText("Enter the same PIN again");
            } else {
                if (enteredPin.equals(firstPin)) {
                    prefs.edit().putString(KEY_MPIN, enteredPin).putBoolean(KEY_MPIN_SET, true).apply();
                    showSuccess("MPIN set!");
                } else {
                    showError("PINs don't match. Try again.");
                    enteredPin = ""; firstPin = ""; isConfirming = false;
                    tvTitle.setText("Set your MPIN");
                    tvSubtitle.setText("Choose a 4-digit MPIN");
                    updateDots();
                }
            }
        } else {
            String saved = prefs.getString(KEY_MPIN, "");
            if (enteredPin.equals(saved)) {
                prefs.edit().putInt(KEY_FAIL_COUNT, 0).apply();
                showSuccess(null);
            } else {
                int fails = prefs.getInt(KEY_FAIL_COUNT, 0) + 1;
                prefs.edit().putInt(KEY_FAIL_COUNT, fails).apply();
                enteredPin = ""; updateDots();
                AnimationHelper.shake(dotRow);
                int remaining = MAX_ATTEMPTS - fails;
                if (remaining <= 0) {
                    long lockUntil = System.currentTimeMillis() + LOCK_DURATION;
                    prefs.edit().putLong(KEY_LOCK_TIME, lockUntil).apply();
                    startLockCountdown(LOCK_DURATION);
                } else {
                    showError("Wrong MPIN!");
                    tvAttempts.setText(remaining + " attempt" + (remaining == 1 ? "" : "s") + " remaining");
                    tvAttempts.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void checkLockout() {
        long lockUntil = getSharedPreferences(PREFS, MODE_PRIVATE).getLong(KEY_LOCK_TIME, 0);
        long remaining = lockUntil - System.currentTimeMillis();
        if (remaining > 0) startLockCountdown(remaining);
    }

    private void startLockCountdown(long ms) {
        keypad.setAlpha(0.3f); keypad.setEnabled(false);
        tvError.setVisibility(View.GONE); tvAttempts.setVisibility(View.GONE);
        tvLockMsg.setVisibility(View.VISIBLE);
        new CountDownTimer(ms, 1000) {
            public void onTick(long left) { tvLockMsg.setText("Too many attempts\nTry again in " + (left/1000) + "s"); }
            public void onFinish() {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putInt(KEY_FAIL_COUNT, 0).remove(KEY_LOCK_TIME).apply();
                tvLockMsg.setVisibility(View.GONE);
                keypad.setAlpha(1f); keypad.setEnabled(true);
            }
        }.start();
    }

    private void showError(String msg) {
        tvError.setText(msg); tvError.setVisibility(View.VISIBLE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> tvError.setVisibility(View.GONE), 2500);
    }

    private void showSuccess(String message) {
        for (View dot : dots) { dot.setBackground(roundedBg(GREEN, 9, this)); AnimationHelper.bounceIn(dot); }
        if (message != null) { tvTitle.setText("✅ " + message); tvTitle.setTextColor(GREEN); }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, HomeActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 600);
    }

    @Override public void onBackPressed() {}
}