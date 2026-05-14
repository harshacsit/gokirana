package com.harsha.kirana;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import static com.harsha.kirana.AppTheme.*;
import static com.harsha.kirana.UiHelper.*;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NotificationHelper.createChannels(this);
        NotificationHelper.scheduleDailyAlarm(this);
        setContentView(buildUI());

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            boolean loggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;
            startActivity(new Intent(this, loggedIn ? MpinActivity.class : LoginActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2200);
    }

    private LinearLayout buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(BG_TOPBAR);   // solid blue background on splash

        // White GK circle logo
        LinearLayout logoBox = new LinearLayout(this);
        logoBox.setGravity(Gravity.CENTER);
        logoBox.setBackground(roundedBg(0xFFFFFFFF, 36, this));
        int sz = dp(this, 100);
        LinearLayout.LayoutParams lbP = new LinearLayout.LayoutParams(sz, sz);
        lbP.gravity = Gravity.CENTER_HORIZONTAL;
        lbP.setMargins(0, 0, 0, dp(this, 28));
        logoBox.setLayoutParams(lbP);

        TextView logoTv = new TextView(this);
        logoTv.setText("GK");
        logoTv.setTextColor(BLUE);
        logoTv.setTextSize(36f);
        logoTv.setTypeface(Typeface.DEFAULT_BOLD);
        logoTv.setGravity(Gravity.CENTER);
        logoBox.addView(logoTv);

        // App name — white on blue
        TextView appName = new TextView(this);
        appName.setText("GoKirana");
        appName.setTextColor(0xFFFFFFFF);
        appName.setTextSize(34f);
        appName.setTypeface(Typeface.DEFAULT_BOLD);
        appName.setGravity(Gravity.CENTER);

        // White underline
        View underline = new View(this);
        underline.setBackground(roundedBg(0xFFFFFFFF, 4, this));
        LinearLayout.LayoutParams ulP = new LinearLayout.LayoutParams(dp(this,60), dp(this,4));
        ulP.gravity = Gravity.CENTER_HORIZONTAL;
        ulP.setMargins(0, dp(this,10), 0, dp(this,14));
        underline.setLayoutParams(ulP);

        // Tagline
        TextView tagline = new TextView(this);
        tagline.setText("Smart Stock · Expiry Alerts · Analytics");
        tagline.setTextColor(0xCCFFFFFF);
        tagline.setTextSize(13f);
        tagline.setTypeface(Typeface.DEFAULT);
        tagline.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tP.setMargins(0, 0, 0, dp(this, 60));
        tagline.setLayoutParams(tP);

        // White loading dots
        TextView loading = new TextView(this);
        loading.setText("● ● ●");
        loading.setTextColor(0xAAFFFFFF);
        loading.setTextSize(10f);
        loading.setGravity(Gravity.CENTER);

        root.addView(logoBox);
        root.addView(appName);
        root.addView(underline);
        root.addView(tagline);
        root.addView(loading);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AnimationHelper.slideUpFadeIn(logoBox,  400, 0);
            AnimationHelper.slideUpFadeIn(appName,  400, 100);
            AnimationHelper.slideUpFadeIn(underline,400, 180);
            AnimationHelper.slideUpFadeIn(tagline,  400, 260);
            AnimationHelper.fadeIn(loading, 500);
        }, 100);

        return root;
    }
}