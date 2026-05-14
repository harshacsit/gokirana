package com.harsha.kirana;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

@IgnoreExtraProperties
public class Product {

    private String id;
    private String name;
    private int    quantity;
    private String expiryDate;
    private int    lowStockThreshold;

    public Product() {}

    public Product(String name, int quantity, String expiryDate, int lowStockThreshold) {
        this.id                = String.valueOf(System.currentTimeMillis());
        this.name              = name;
        this.quantity          = quantity;
        this.expiryDate        = expiryDate;
        this.lowStockThreshold = lowStockThreshold;
    }

    public static String daysToDateString(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, days);
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.getTime());
    }

    public static String todayAsString() {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
    }

    public String getId()                { return id; }
    public String getName()              { return name; }
    public int    getQuantity()          { return quantity; }
    public String getExpiryDate()        { return expiryDate; }
    public int    getLowStockThreshold() { return lowStockThreshold; }
    public void setId(String id)                  { this.id = id; }
    public void setName(String name)              { this.name = name; }
    public void setQuantity(int q)                { this.quantity = q; }
    public void setExpiryDate(String d)           { this.expiryDate = d; }
    public void setLowStockThreshold(int t)       { this.lowStockThreshold = t; }

    @Exclude
    public int daysUntilExpiry() {
        if (expiryDate == null || expiryDate.trim().isEmpty()) return Integer.MAX_VALUE;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            sdf.setLenient(false);
            Date expiry = sdf.parse(expiryDate.trim());
            if (expiry == null) return Integer.MAX_VALUE;
            Calendar expCal = Calendar.getInstance();
            expCal.setTime(expiry);
            expCal.set(Calendar.HOUR_OF_DAY, 0); expCal.set(Calendar.MINUTE, 0);
            expCal.set(Calendar.SECOND, 0);       expCal.set(Calendar.MILLISECOND, 0);
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);   today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);         today.set(Calendar.MILLISECOND, 0);
            return (int)((expCal.getTimeInMillis() - today.getTimeInMillis()) / (1000L * 60 * 60 * 24));
        } catch (Exception e) { return Integer.MAX_VALUE; }
    }

    @Exclude public boolean isExpired()          { return daysUntilExpiry() < 0; }
    @Exclude public boolean isExpiringSoon()     { int d = daysUntilExpiry(); return d >= 0 && d <= 3; }
    @Exclude public boolean isExpiringThisWeek() { int d = daysUntilExpiry(); return d >= 0 && d <= 7; }
    @Exclude public boolean isLowStock()         { return quantity >= 0 && quantity <= lowStockThreshold; }

    @Exclude
    public String statusLabel() {
        int d = daysUntilExpiry();
        if (d == Integer.MAX_VALUE) { return isLowStock() ? "Low Stock" : "In Stock"; }
        if (d < 0)  return "EXPIRED (" + Math.abs(d) + "d ago)";
        if (d == 0) return "⚠ Expires TODAY!";
        if (d == 1) return "⚠ Expires TOMORROW!";
        if (d == 2) return "⚠ Expires in 2 days";
        if (d == 3) return "⚠ Expires in 3 days";
        if (d <= 7) return "Expires in " + d + " days";
        if (isLowStock()) return "Low Stock";
        return "In Stock";
    }

    @Exclude
    public int accentColor() {
        if (isExpired() || isExpiringSoon()) return AppTheme.RED;
        if (isLowStock() || isExpiringThisWeek()) return AppTheme.AMBER;
        return AppTheme.GREEN;
    }

    @Exclude
    public int accentDimColor() {
        if (isExpired() || isExpiringSoon()) return AppTheme.RED_DIM;
        if (isLowStock() || isExpiringThisWeek()) return AppTheme.AMBER_DIM;
        return AppTheme.GREEN_DIM;
    }
}