package com.harsha.kirana;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import java.util.List;
import java.util.Locale;

public class VoiceAlertHelper {

    private static final String TAG = "Voice";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static TextToSpeech tts;
    private static boolean ready    = false;
    private static boolean initing  = false;
    private static boolean telugu   = false;

    // One pending message while TTS initialises
    private static Runnable pendingSpeak = null;

    public static void init(Context ctx) {
        if (ready || initing) return;
        initing = true;
        // Must create TTS on main thread
        MAIN.post(() -> tts = new TextToSpeech(ctx.getApplicationContext(), status -> {
            initing = false;
            if (status != TextToSpeech.SUCCESS) { Log.e(TAG, "TTS init failed"); return; }
            int r = tts.setLanguage(new Locale("te", "IN"));
            telugu = (r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED);
            if (!telugu) tts.setLanguage(Locale.ENGLISH);
            tts.setSpeechRate(0.82f);
            tts.setPitch(1.05f);
            ready = true;
            Log.d(TAG, "TTS ready. Telugu=" + telugu);
            if (pendingSpeak != null) { pendingSpeak.run(); pendingSpeak = null; }
        }));
    }

    public static void speakExpiryAlert(Context ctx, List<Product> products) {
        if (products == null || products.isEmpty()) return;
        init(ctx);
        Runnable speak = () -> doSpeak(buildExpiry(products));
        MAIN.post(() -> { if (ready) speak.run(); else pendingSpeak = speak; });
    }

    public static void speakLowStockAlert(Context ctx, List<Product> products) {
        if (products == null || products.isEmpty()) return;
        init(ctx);
        Runnable speak = () -> doSpeak(buildLowStock(products));
        MAIN.post(() -> { if (ready) speak.run(); else pendingSpeak = speak; });
    }

    private static void doSpeak(String text) {
        if (tts == null || !ready) return;
        Log.d(TAG, "Speaking: " + text);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kirana_alert");
    }

    private static String buildExpiry(List<Product> products) {
        StringBuilder s = new StringBuilder();
        if (telugu) {
            s.append("గో కిరాణా హెచ్చరిక. ");
            Product p = products.get(0);
            int d = p.daysUntilExpiry();
            s.append(p.getName()).append(". ");
            if (d < 0)       s.append("గడువు అయిపోయింది. వెంటనే తొలగించండి.");
            else if (d == 0) s.append("ఈరోజే గడువు. వెంటనే అమ్మండి.");
            else if (d == 1) s.append("రేపు గడువు. నేడే అమ్మండి.");
            else             s.append(d).append(" రోజుల్లో గడువు. జాగ్రత్త పడండి.");
            if (products.size() > 1)
                s.append(" మరియు ").append(products.size()-1).append(" ఇతర వస్తువులు.");
        } else {
            s.append("GoKirana alert. ");
            Product p = products.get(0);
            int d = p.daysUntilExpiry();
            s.append(p.getName()).append(". ");
            if (d < 0) s.append("Expired. Remove it.");
            else if (d == 0) s.append("Expires today. Sell now.");
            else s.append("Expires in ").append(d).append(" days.");
        }
        return s.toString();
    }

    private static String buildLowStock(List<Product> products) {
        StringBuilder s = new StringBuilder();
        if (telugu) {
            s.append("గో కిరాణా హెచ్చరిక. ");
            Product p = products.get(0);
            s.append(p.getName()).append(". స్టాక్ తక్కువగా ఉంది. కేవలం ")
                    .append(p.getQuantity()).append(" మాత్రమే మిగిలాయి. వెంటనే ఆర్డర్ చేయండి.");
            if (products.size() > 1)
                s.append(" మరియు ").append(products.size()-1).append(" ఇతర వస్తువులు.");
        } else {
            s.append("GoKirana alert. ");
            Product p = products.get(0);
            s.append(p.getName()).append(". Only ").append(p.getQuantity()).append(" left. Reorder now.");
        }
        return s.toString();
    }

    public static void stop() {
        pendingSpeak = null;
        MAIN.post(() -> { if (tts != null && ready) tts.stop(); });
    }

    public static void shutdown() {
        pendingSpeak = null;
        ready = false; initing = false;
        MAIN.post(() -> { if (tts != null) { tts.stop(); tts.shutdown(); tts = null; } });
    }
}