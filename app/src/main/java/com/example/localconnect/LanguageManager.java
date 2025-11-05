// app/src/main/java/com/example/localconnect/LanguageManager.java
package com.example.localconnect;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

/**
 *  runtime language switcher using SharedPreferences .
 */
public class LanguageManager {
    private static final String PREFS = "localconnect_lang_prefs";
    private static final String KEY_LANG = "lang";

    public static void applySavedLocale(Context context) {
        String lang = getSavedLocale(context);
        if (lang == null || lang.isEmpty()) return;
        setLocale(context, lang, false);
    }

    public static void setLocale(Context context, String langCode, boolean save) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Resources res = context.getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());

        if (save) {
            SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit().putString(KEY_LANG, langCode).apply();
        }
    }

    public static String getSavedLocale(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getString(KEY_LANG, "en");
    }
}
