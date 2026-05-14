package com.harsha.kirana;

import android.content.Context;

public class LangHelper {

    private static final String PREFS    = "kirana_prefs";
    private static final String KEY_LANG = "app_language";
    public  static final String TELUGU   = "te";
    public  static final String ENGLISH  = "en";

    public static String current(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_LANG, ENGLISH);
    }

    public static boolean isTelugu(Context ctx) {
        return TELUGU.equals(current(ctx));
    }

    public static void toggle(Context ctx) {
        String next = isTelugu(ctx) ? ENGLISH : TELUGU;
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_LANG, next).apply();
    }

    public static String currentLangName(Context ctx) {
        return isTelugu(ctx) ? "తెలుగు" : "English";
    }

    public static String t(Context ctx, String key) {
        boolean te = isTelugu(ctx);
        switch (key) {
            case "app_name":        return te ? "గో కిరాణా"        : "GoKirana";
            case "good_morning":    return te ? "శుభోదయం ☀️"       : "Good morning ☀️";
            case "good_afternoon":  return te ? "శుభ మధ్యాహ్నం 👋" : "Good afternoon 👋";
            case "good_evening":    return te ? "శుభ సాయంత్రం 🌙"  : "Good evening 🌙";
            case "save":            return te ? "సేవ్ చేయి"         : "Save";
            case "cancel":          return te ? "రద్దు చేయి"        : "Cancel";
            case "delete":          return te ? "తొలగించు"          : "Delete";
            case "edit":            return te ? "సవరించు"           : "Edit";
            case "loading":         return te ? "లోడ్ అవుతోంది..."  : "Loading...";
            case "search":          return te ? "వస్తువులు వెతకండి" : "Search products";
            case "settings":        return te ? "సెట్టింగులు"       : "Settings";
            case "logout":          return te ? "లాగ్ అవుట్"        : "Logout";
            case "login":           return te ? "లాగిన్"            : "Login";
            case "phone":           return te ? "ఫోన్ నంబర్"        : "Phone Number";
            case "password":        return te ? "పాస్వర్డ్"          : "Password";
            case "enter_mpin":      return te ? "MPIN నమోదు చేయండి"  : "Enter MPIN";
            case "set_mpin":        return te ? "MPIN సెట్ చేయండి"   : "Set your MPIN";
            case "wrong_mpin":      return te ? "తప్పు MPIN!"        : "Wrong MPIN!";
            case "not_you":         return te ? "మీరు కాదా? లాగ్ అవుట్" : "Not you? Logout";
            case "confirm_mpin":    return te ? "MPIN నిర్ధారించండి" : "Confirm MPIN";
            case "store_suffix":    return te ? " కిరాణా స్టోర్"     : "'s Store";
            case "products":        return te ? "వస్తువులు"          : "Products";
            case "low_stock":       return te ? "తక్కువ స్టాక్"      : "Low Stock";
            case "expiring":        return te ? "గడువు మీరుతున్నాయి"  : "Expiring";
            case "quick_actions":   return te ? "త్వరిత చర్యలు"     : "Quick Actions";
            case "stock":           return te ? "స్టాక్"              : "Stock";
            case "analytics":       return te ? "విశ్లేషణలు"         : "Analytics";
            case "alerts":          return te ? "హెచ్చరికలు"         : "Alerts";
            case "stock_manager":   return te ? "స్టాక్ నిర్వహణ"    : "Stock Manager";
            case "add_product":     return te ? "వస్తువు జోడించు"   : "Add Product";
            case "edit_product":    return te ? "వస్తువు సవరించు"   : "Edit Product";
            case "product_name":    return te ? "వస్తువు పేరు"      : "Product Name";
            case "expiry_days":     return te ? "ఎన్ని రోజుల్లో గడువు?" : "Expires in how many days?";
            case "quantity":        return te ? "పరిమాణం"            : "Quantity";
            case "alert_at":        return te ? "ఎంత వద్ద హెచ్చరించాలి?" : "Alert when stock reaches";
            case "sell":            return te ? "అమ్ము"              : "Sell";
            case "buy":             return te ? "కొను"               : "Buy";
            case "remove":          return te ? "తొలగించు"           : "Remove";
            case "in_stock":        return te ? "స్టాక్‌లో ఉంది"    : "In Stock";
            case "all":             return te ? "అన్నీ"              : "All";
            case "no_products":     return te ? "వస్తువులు లేవు"    : "No products yet";
            case "add_hint":        return te ? "+ జోడించు నొక్కండి" : "Tap + Add to start";
            case "expiry_hint":     return te ? "0=నేడు · 7=వారం · 30=నెల · ఖాళీ=గడువు లేదు"
                    : "0=today · 7=1 week · 30=1 month · blank=no expiry";
            case "save_product":    return te ? "వస్తువు సేవ్ చేయి" : "Save Product";
            case "update_product":  return te ? "వస్తువు అప్డేట్ చేయి" : "Update Product";
            case "remove_confirm":  return te ? "తొలగించాలా?"        : "Remove product?";
            case "remove_msg":      return te ? "శాశ్వతంగా తొలగించబడుతుంది" : "This will be permanently deleted.";
            case "expired":         return te ? "గడువు అయిపోయింది"  : "Expired";
            case "expires_today":   return te ? "ఈరోజే గడువు!"      : "Expires TODAY!";
            case "expires_tom":     return te ? "రేపు గడువు!"        : "Expires TOMORROW!";
            case "expires_2d":      return te ? "2 రోజుల్లో గడువు"  : "Expires in 2 days";
            case "low_stock_alert": return te ? "తక్కువ స్టాక్ హెచ్చరిక" : "Low Stock Warning";
            case "expiry_alert":    return te ? "గడువు హెచ్చరిక"   : "Expiry Warning";
            case "profile":         return te ? "ప్రొఫైల్"           : "Profile";
            case "security":        return te ? "భద్రత"              : "Security";
            case "about":           return te ? "యాప్ గురించి"       : "About";
            case "change_mpin":     return te ? "MPIN మార్చు"        : "Change MPIN";
            case "tap_photo":       return te ? "ఫోటో మార్చడానికి నొక్కండి" : "Tap photo to change";
            case "photo_updated":   return te ? "ఫోటో అప్డేట్ అయింది!" : "Photo updated!";
            case "language":        return te ? "భాష"                : "Language";
            case "switch_to":       return te ? "English కి మార్చు"  : "తెలుగుకి మార్చు";
            case "logout_confirm":  return te ? "లాగ్ అవుట్ అవ్వాలా?" : "Logout?";
            case "logout_msg":      return te ? "మళ్ళీ లాగిన్ చేయాలి" : "You will need to login again.";
            case "mpin_active":     return te ? "MPIN యాక్టివ్‌గా ఉంది" : "MPIN is active";
            case "version":         return te ? "వెర్షన్"             : "Version";
            case "backend":         return te ? "బ్యాకెండ్"           : "Backend";
            case "daily_alert":     return te ? "ప్రతి రోజు ఉదయం 9 గంటలకు" : "Daily alerts at 9 AM";
            case "mpin_match_err":  return te ? "MPIN సరిపోలలేదు"   : "PINs don't match";
            default: return key;
        }
    }
}