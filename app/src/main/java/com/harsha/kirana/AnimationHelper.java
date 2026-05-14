package com.harsha.kirana;

import android.animation.*;
import android.view.View;
import android.view.animation.*;

public class AnimationHelper {

    // ── Fade in a view from invisible ─────────────────────────────────────
    public static void fadeIn(View view, int durationMs) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1f)
                .setDuration(durationMs)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    // ── Slide up + fade in (for cards entering screen) ────────────────────
    public static void slideUpFadeIn(View view, int durationMs, int delayMs) {
        view.setAlpha(0f);
        view.setTranslationY(60f);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(durationMs)
                .setStartDelay(delayMs)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
    }

    // ── Staggered slide up for a list of views ────────────────────────────
    public static void staggeredSlideUp(View[] views, int baseDelay) {
        for (int i = 0; i < views.length; i++) {
            slideUpFadeIn(views[i], 350, baseDelay + (i * 60));
        }
    }

    // ── Scale pulse (for buttons on press) ────────────────────────────────
    public static void pulse(View view) {
        view.animate()
                .scaleX(0.94f).scaleY(0.94f)
                .setDuration(80)
                .withEndAction(() ->
                        view.animate()
                                .scaleX(1f).scaleY(1f)
                                .setDuration(120)
                                .setInterpolator(new OvershootInterpolator(2f))
                                .start()
                ).start();
    }

    // ── Shake animation (for wrong MPIN / validation error) ───────────────
    public static void shake(View view) {
        ObjectAnimator shaker = ObjectAnimator.ofFloat(
                view, "translationX",
                0f, -18f, 18f, -14f, 14f, -10f, 10f, -6f, 6f, 0f
        );
        shaker.setDuration(500);
        shaker.setInterpolator(new DecelerateInterpolator());
        shaker.start();
    }

    // ── Flash red (for error highlight) ───────────────────────────────────
    public static void flashError(View view) {
        ValueAnimator colorAnim = ValueAnimator.ofArgb(
                0xFF1C1C1C, 0x44E03E3E, 0xFF1C1C1C
        );
        colorAnim.setDuration(600);
        colorAnim.addUpdateListener(a ->
                view.setBackgroundColor((int) a.getAnimatedValue()));
        colorAnim.start();
    }

    // ── Bounce in (for MPIN dot filling) ─────────────────────────────────
    public static void bounceIn(View view) {
        view.setScaleX(0f);
        view.setScaleY(0f);
        view.animate()
                .scaleX(1f).scaleY(1f)
                .setDuration(200)
                .setInterpolator(new OvershootInterpolator(3f))
                .start();
    }

    // ── Shrink out (for MPIN dot clearing) ───────────────────────────────
    public static void shrinkOut(View view) {
        view.animate()
                .scaleX(0f).scaleY(0f)
                .setDuration(150)
                .setInterpolator(new AccelerateInterpolator())
                .start();
    }

    // ── Loading spinner show/hide ─────────────────────────────────────────
    public static void showLoading(View spinner, View content) {
        fadeIn(spinner, 200);
        content.animate().alpha(0.3f).setDuration(200).start();
    }

    public static void hideLoading(View spinner, View content) {
        spinner.animate().alpha(0f).setDuration(150)
                .withEndAction(() -> spinner.setVisibility(View.GONE)).start();
        content.animate().alpha(1f).setDuration(200).start();
    }

    // ── Success checkmark bounce ───────────────────────────────────────────
    public static void successBounce(View view) {
        view.setScaleX(0f);
        view.setScaleY(0f);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .scaleX(1.2f).scaleY(1.2f)
                .setDuration(200)
                .withEndAction(() ->
                        view.animate()
                                .scaleX(1f).scaleY(1f)
                                .setDuration(100)
                                .start()
                ).start();
    }
}