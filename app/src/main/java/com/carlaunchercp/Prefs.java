package com.carlaunchercp;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/** Small persisted settings store: theme, configured apps, favourite shortcuts. */
public final class Prefs {

    public static final int MAX_FAVORITES = 6;

    private static final String FILE = "car_launcher_cp";
    private static final String K_THEME = "theme_id";
    private static final String K_RADIO = "radio_component";
    private static final String K_MUSIC = "music_component";
    private static final String K_FAVORITES = "favorites";

    private final SharedPreferences sp;

    public Prefs(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public String themeId() {
        return sp.getString(K_THEME, ThemePalette.ROSSO.id);
    }

    public void setThemeId(String id) {
        sp.edit().putString(K_THEME, id).apply();
    }

    public String radioTarget() {
        return sp.getString(K_RADIO, null);
    }

    public void setRadioTarget(String flattened) {
        sp.edit().putString(K_RADIO, flattened).apply();
    }

    public String musicTarget() {
        return sp.getString(K_MUSIC, null);
    }

    public void setMusicTarget(String flattened) {
        sp.edit().putString(K_MUSIC, flattened).apply();
    }

    public List<String> favorites() {
        String raw = sp.getString(K_FAVORITES, "");
        List<String> result = new ArrayList<>();
        if (!TextUtils.isEmpty(raw)) {
            for (String entry : raw.split(";")) {
                if (!entry.isEmpty()) result.add(entry);
            }
        }
        return result;
    }

    public void addFavorite(String flattened) {
        List<String> current = favorites();
        if (flattened == null || current.contains(flattened) || current.size() >= MAX_FAVORITES) return;
        current.add(flattened);
        saveFavorites(current);
    }

    public void replaceFavorite(int index, String flattened) {
        List<String> current = favorites();
        if (flattened == null || index < 0 || index >= current.size()) return;
        current.set(index, flattened);
        saveFavorites(current);
    }

    public void removeFavorite(int index) {
        List<String> current = favorites();
        if (index < 0 || index >= current.size()) return;
        current.remove(index);
        saveFavorites(current);
    }

    private void saveFavorites(List<String> favorites) {
        sp.edit().putString(K_FAVORITES, TextUtils.join(";", favorites)).apply();
    }
}
