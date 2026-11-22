package com.carlaunchercp;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

/** Applies the selected launcher language to an activity context. */
public final class LocaleHelper {

    public static final String IT = "it";
    public static final String EN = "en";

    private LocaleHelper() {
    }

    public static Context wrap(Context context, String tag) {
        Locale locale = new Locale(EN.equals(tag) ? EN : IT);
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);
        return context.createConfigurationContext(config);
    }
}
