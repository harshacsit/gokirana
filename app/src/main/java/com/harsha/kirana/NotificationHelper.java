package com.harsha.kirana;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class NotificationHelper {

    private static final String CHANNEL_EXPIRY   = "kirana_expiry";
    private static final String CHANNEL_LOWSTOCK  = "kirana_lowstock";
    private static final int    NOTIF_EXPIRY_ID   = 1001;
    private static final int    NOTIF_STOCK_ID    = 1002;
    private static final int    NOTIF_SUMMARY_ID  = 1003;

    // Alarm request codes — one per time slot
    private static final int ALARM_8AM = 2001;
    private static final int ALARM_1PM = 2002;
    private static final int ALARM_7PM = 2003;

    // SharedPrefs keys
    private static final String PREFS            = "kirana_prefs";
    private static final String KEY_LAST_WA_DATE = "last_whatsapp_date";

    // ── Create notification channels (required Android 8+) ────────────────
    public static void createChannels(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm == null) return;

            NotificationChannel exp = new NotificationChannel(
                    CHANNEL_EXPIRY, "Expiry Alerts", NotificationManager.IMPORTANCE_HIGH);
            exp.setDescription("Products expiring soon");
            exp.enableVibration(true);
            nm.createNotificationChannel(exp);

            NotificationChannel stock = new NotificationChannel(
                    CHANNEL_LOWSTOCK, "Low Stock Alerts", NotificationManager.IMPORTANCE_HIGH);
            stock.setDescription("Stock running low");
            stock.enableVibration(true);
            nm.createNotificationChannel(stock);
        }
    }

    // ── Schedule all 3 daily alarms ────────────────────────────────────────
    public static void scheduleAllAlarms(Context ctx) {
        scheduleAt(ctx, ALARM_8AM,  8,  0);
        scheduleAt(ctx, ALARM_1PM, 13,  0);
        scheduleAt(ctx, ALARM_7PM, 19,  0);
    }

    // Called from SplashActivity
    public static void scheduleDailyAlarm(Context ctx) {
        createChannels(ctx);
        scheduleAllAlarms(ctx);
    }

    // ── Schedule one repeating daily alarm at hour:minute ──────────────────
    private static void scheduleAt(Context ctx, int code, int hour, int minute) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(ctx, SmartAlarmReceiver.class);
        intent.putExtra("alarm_code", code);
        intent.putExtra("hour", hour);

        PendingIntent pi = PendingIntent.getBroadcast(ctx, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis())
            cal.add(Calendar.DAY_OF_YEAR, 1);

        am.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
    }

    // ── Send expiry push notification ──────────────────────────────────────
    public static void sendExpiryNotification(Context ctx, List<Product> items) {
        if (items == null || items.isEmpty()) return;

        StringBuilder body = new StringBuilder();
        for (int i = 0; i < Math.min(3, items.size()); i++) {
            Product p = items.get(i);
            int d = p.daysUntilExpiry();
            String ds = d < 0 ? "EXPIRED!" : d == 0 ? "TODAY!" : d == 1 ? "Tomorrow!" : "in " + d + " days";
            body.append("• ").append(p.getName()).append(" — ").append(ds).append("\n");
        }
        if (items.size() > 3) body.append("+ ").append(items.size() - 3).append(" more");

        String title = items.size() == 1
                ? "⚠️ " + items.get(0).getName() + " expiring soon!"
                : "⚠️ " + items.size() + " products expiring soon!";

        showNotification(ctx, CHANNEL_EXPIRY, NOTIF_EXPIRY_ID, title,
                body.toString().trim(), 0xFFD32F2F);
    }

    // ── Send low stock push notification ───────────────────────────────────
    public static void sendLowStockNotification(Context ctx, List<Product> items) {
        if (items == null || items.isEmpty()) return;

        StringBuilder body = new StringBuilder();
        for (int i = 0; i < Math.min(3, items.size()); i++) {
            Product p = items.get(i);
            body.append("• ").append(p.getName())
                    .append(" — only ").append(p.getQuantity()).append(" left\n");
        }
        if (items.size() > 3) body.append("+ ").append(items.size() - 3).append(" more");

        String title = items.size() == 1
                ? "📦 " + items.get(0).getName() + " running low!"
                : "📦 " + items.size() + " items running low!";

        showNotification(ctx, CHANNEL_LOWSTOCK, NOTIF_STOCK_ID, title,
                body.toString().trim(), 0xFF1A73E8);
    }

    // ── Send morning summary notification ─────────────────────────────────
    private static void sendSummaryNotification(Context ctx, int expiring, int lowStock) {
        if (expiring == 0 && lowStock == 0) return;
        StringBuilder body = new StringBuilder();
        if (expiring > 0)  body.append("🗓 ").append(expiring).append(" product(s) expiring soon\n");
        if (lowStock > 0)  body.append("📦 ").append(lowStock).append(" item(s) running low");
        showNotification(ctx, CHANNEL_EXPIRY, NOTIF_SUMMARY_ID,
                "☀️ GoKirana Morning Update", body.toString().trim(), 0xFF1A73E8);
    }

    // ── Core notification sender ───────────────────────────────────────────
    private static void showNotification(Context ctx, String channel, int id,
                                         String title, String body, int color) {
        Intent intent = new Intent(ctx, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pi = PendingIntent.getActivity(ctx, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, channel)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(color)
                .setContentIntent(pi)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(ctx).notify(id, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // SMART ALARM RECEIVER — fires at 8AM, 1PM, 7PM
    // ══════════════════════════════════════════════════════════════════════
    public static class SmartAlarmReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context ctx, Intent intent) {
            int alarmCode = intent.getIntExtra("alarm_code", ALARM_8AM);

            com.google.firebase.auth.FirebaseUser user =
                    com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            // Load products from Firestore
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .collection("products")
                    .get()
                    .addOnSuccessListener(query -> {

                        List<Product> allExpiring  = new ArrayList<>();
                        List<Product> critical     = new ArrayList<>(); // today/tomorrow
                        List<Product> allLowStock  = new ArrayList<>();
                        List<Product> criticalLow  = new ArrayList<>(); // qty == 1

                        for (com.google.firebase.firestore.DocumentSnapshot doc
                                : query.getDocuments()) {
                            Product p = doc.toObject(Product.class);
                            if (p == null) continue;
                            p.setId(doc.getId());

                            if (p.isExpiringSoon() || p.isExpired()) {
                                allExpiring.add(p);
                                if (p.daysUntilExpiry() <= 1) critical.add(p);
                            }
                            if (p.isLowStock()) {
                                allLowStock.add(p);
                                if (p.getQuantity() <= 1) criticalLow.add(p);
                            }
                        }

                        if (alarmCode == ALARM_8AM) {
                            // ── 8 AM: full summary + WhatsApp ─────────────────
                            sendSummaryNotification(ctx, allExpiring.size(), allLowStock.size());
                            if (!allExpiring.isEmpty()) sendExpiryNotification(ctx, allExpiring);
                            if (!allLowStock.isEmpty()) sendLowStockNotification(ctx, allLowStock);

                            // WhatsApp — once per day only
                            String today = new java.text.SimpleDateFormat(
                                    "yyyy-MM-dd", java.util.Locale.getDefault())
                                    .format(new java.util.Date());
                            SharedPreferences prefs =
                                    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                            if (!today.equals(prefs.getString(KEY_LAST_WA_DATE, ""))) {
                                // Send WhatsApp to this user's own number via Twilio
                                if (!allExpiring.isEmpty())
                                    WhatsAppHelper.sendExpiryAlert(allExpiring);
                                if (!allLowStock.isEmpty())
                                    WhatsAppHelper.sendLowStockAlert(allLowStock);
                                prefs.edit().putString(KEY_LAST_WA_DATE, today).apply();
                            }

                        } else if (alarmCode == ALARM_1PM) {
                            // ── 1 PM: critical only ────────────────────────────
                            if (!critical.isEmpty())    sendExpiryNotification(ctx, critical);
                            if (!criticalLow.isEmpty()) sendLowStockNotification(ctx, criticalLow);

                        } else {
                            // ── 7 PM: evening reminder ─────────────────────────
                            if (!allExpiring.isEmpty()) sendExpiryNotification(ctx, allExpiring);
                            if (!allLowStock.isEmpty()) sendLowStockNotification(ctx, allLowStock);
                        }
                    });
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // BOOT RECEIVER — reschedule alarms after phone restart
    // ══════════════════════════════════════════════════════════════════════
    public static class BootReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                createChannels(ctx);
                scheduleAllAlarms(ctx);
            }
        }
    }
}