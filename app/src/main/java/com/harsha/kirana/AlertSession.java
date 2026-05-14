package com.harsha.kirana;

import java.util.HashSet;
import java.util.Set;

/**
 * App-wide singleton. Lives for the entire session.
 * Survives Activity recreation, screen switches, language toggle.
 * Only reset on logout.
 */
public class AlertSession {

    private static AlertSession instance;

    private final Set<String> shownLowStock = new HashSet<>();
    private final Set<String> shownExpiry   = new HashSet<>();
    private boolean startupDone = false;

    private AlertSession() {}

    public static AlertSession get() {
        if (instance == null) instance = new AlertSession();
        return instance;
    }

    // Low stock
    public boolean isLowStockShown(String id)    { return id != null && shownLowStock.contains(id); }
    public void    markLowStockShown(String id)  { if (id != null) shownLowStock.add(id); }
    public void    clearLowStockShown(String id) { if (id != null) shownLowStock.remove(id); }

    // Expiry
    public boolean isExpiryShown(String id)      { return id != null && shownExpiry.contains(id); }
    public void    markExpiryShown(String id)    { if (id != null) shownExpiry.add(id); }

    // Startup
    public boolean wasStartupDone()  { return startupDone; }
    public void    markStartupDone() { startupDone = true; }

    // Reset on logout
    public static void reset() { instance = null; }
}