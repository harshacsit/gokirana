package com.harsha.kirana;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import static com.harsha.kirana.AppTheme.*;

public class UiHelper {

    public static int dp(Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable roundedBg(int color, int radiusDp, Context ctx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(ctx, radiusDp));
        return d;
    }

    public static GradientDrawable roundedStroke(int fillColor, int strokeColor,
                                                 int radiusDp, int strokeDp, Context ctx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fillColor);
        d.setCornerRadius(dp(ctx, radiusDp));
        d.setStroke(dp(ctx, strokeDp), strokeColor);
        return d;
    }

    public static GradientDrawable ovalBg(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    // ── Blue top bar (replaces dark navy) ─────────────────────────────────
    public static LinearLayout topBar(Context ctx, String title, String subtitle) {
        LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setBackgroundColor(BG_TOPBAR);
        bar.setPadding(dp(ctx,18), dp(ctx,48), dp(ctx,18), dp(ctx,20));

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        // White left accent bar
        View accent = new View(ctx);
        accent.setBackgroundColor(0xFFFFFFFF);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dp(ctx,3), dp(ctx,28));
        ap.setMargins(0,0,dp(ctx,12),0);
        accent.setLayoutParams(ap);

        LinearLayout textCol = new LinearLayout(ctx);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(title);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTextSize(20f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        textCol.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(ctx);
            tvSub.setText(subtitle);
            tvSub.setTextColor(0xCCFFFFFF);
            tvSub.setTextSize(12f);
            textCol.addView(tvSub);
        }

        row.addView(accent);
        row.addView(textCol);
        bar.addView(row);
        return bar;
    }

    // ── Blue primary button ────────────────────────────────────────────────
    public static Button orangeButton(Context ctx, String label) {
        Button btn = new Button(ctx);
        btn.setText(label);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTextSize(15f);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setBackground(roundedBg(BLUE, 14, ctx));
        btn.setAllCaps(false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 54));
        btn.setLayoutParams(p);
        return btn;
    }

    public static Button ghostButton(Context ctx, String label, int color) {
        Button btn = new Button(ctx);
        btn.setText(label);
        btn.setTextColor(color);
        btn.setTextSize(14f);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setBackground(roundedStroke(BG_CARD, color, 14, 1, ctx));
        btn.setAllCaps(false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 54));
        btn.setLayoutParams(p);
        return btn;
    }

    // ── Light input field ──────────────────────────────────────────────────
    public static EditText darkField(Context ctx, String hint, int inputType) {
        EditText et = new EditText(ctx);
        et.setHint(hint);
        et.setHintTextColor(TEXT_HINT);
        et.setTextColor(TEXT_WHITE);   // dark text on light field
        et.setTextSize(14f);
        et.setInputType(inputType);
        et.setBackground(roundedStroke(BG_FIELD, BLACK_BORDER, 12, 1, ctx));
        et.setPadding(dp(ctx,16), dp(ctx,16), dp(ctx,16), dp(ctx,16));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 56));
        p.setMargins(0, 0, 0, dp(ctx, 12));
        et.setLayoutParams(p);
        return et;
    }

    // ── Section label ──────────────────────────────────────────────────────
    public static TextView sectionLabel(Context ctx, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextColor(TEXT_MUTED);
        tv.setTextSize(10f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(dp(ctx,16), dp(ctx,20), dp(ctx,16), dp(ctx,8));
        tv.setLayoutParams(p);
        return tv;
    }

    // ── Alert banner ───────────────────────────────────────────────────────
    public static LinearLayout alertBanner(Context ctx, String icon, String message,
                                           int bgColor, int borderColor, int textColor) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(roundedBg(bgColor, 10, ctx));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.setMargins(dp(ctx,14), dp(ctx,8), dp(ctx,14), 0);
        row.setLayoutParams(rp);

        View bar = new View(ctx);
        bar.setBackground(roundedBg(borderColor, 3, ctx));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(ctx,3), dp(ctx,38));
        bp.setMargins(0,0,dp(ctx,10),0);
        bar.setLayoutParams(bp);

        TextView iconTv = new TextView(ctx);
        iconTv.setText(icon);
        iconTv.setTextSize(13f);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ip.setMargins(0,0,dp(ctx,6),0);
        iconTv.setLayoutParams(ip);

        TextView msgTv = new TextView(ctx);
        msgTv.setText(message);
        msgTv.setTextColor(textColor);
        msgTv.setTextSize(12f);
        msgTv.setTypeface(null, Typeface.BOLD);

        row.addView(bar); row.addView(iconTv); row.addView(msgTv);
        return row;
    }

    // ── Bottom navigation bar — WHITE background ───────────────────────────
    public static LinearLayout bottomNav(Context ctx, String[] labels, String[] icons,
                                         int activeIndex, Runnable[] onClicks) {
        LinearLayout wrapper = new LinearLayout(ctx);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        wrapper.setBackgroundColor(BG_CARD);

        // Top border — light blue
        View border = new View(ctx);
        border.setBackgroundColor(BLACK_BORDER);
        border.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx,1)));
        wrapper.addView(border);

        LinearLayout inner = new LinearLayout(ctx);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setPadding(0, dp(ctx,8), 0, dp(ctx,10));
        inner.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            LinearLayout item = new LinearLayout(ctx);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            item.setClickable(true); item.setFocusable(true);
            item.setOnClickListener(v -> { if (onClicks[idx] != null) onClicks[idx].run(); });

            TextView iconTv = new TextView(ctx);
            iconTv.setText(icons[i]);
            iconTv.setTextSize(18f);
            iconTv.setGravity(Gravity.CENTER);

            TextView labelTv = new TextView(ctx);
            labelTv.setText(labels[i]);
            labelTv.setTextSize(9f);
            labelTv.setGravity(Gravity.CENTER);
            labelTv.setTypeface(null, Typeface.BOLD);

            if (i == activeIndex) {
                labelTv.setTextColor(BLUE);
                View dot = new View(ctx);
                dot.setBackground(ovalBg(BLUE));
                LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(dp(ctx,4), dp(ctx,4));
                dp2.gravity = Gravity.CENTER_HORIZONTAL;
                dp2.setMargins(0, dp(ctx,2), 0, 0);
                dot.setLayoutParams(dp2);
                item.addView(iconTv); item.addView(labelTv); item.addView(dot);
            } else {
                labelTv.setTextColor(TEXT_MUTED);
                item.addView(iconTv); item.addView(labelTv);
            }
            inner.addView(item);
        }
        wrapper.addView(inner);
        return wrapper;
    }
}