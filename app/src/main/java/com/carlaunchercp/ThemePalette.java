package com.carlaunchercp;

import android.graphics.Color;

/**
 * Selectable colour themes. Every surface in the launcher is tinted from here,
 * so switching a theme repaints background glow, gauge, icons and highlights.
 */
public final class ThemePalette {

    public final String id;
    public final String label;
    public final int accent;

    private ThemePalette(String id, String label, int accent) {
        this.id = id;
        this.label = label;
        this.accent = accent;
    }

    public static final ThemePalette ROSSO = new ThemePalette("red", "Rosso", 0xFFFF3B30);
    public static final ThemePalette CORALLO = new ThemePalette("coral", "Corallo", 0xFFFF5A3C);
    public static final ThemePalette ARANCIO = new ThemePalette("orange", "Arancio", 0xFFFF7A18);
    public static final ThemePalette AMBRA = new ThemePalette("amber", "Ambra", 0xFFFFB020);
    public static final ThemePalette VERDE = new ThemePalette("green", "Verde", 0xFF3FDB86);
    public static final ThemePalette TURCHESE = new ThemePalette("turquoise", "Turchese", 0xFF1FC9C0);
    public static final ThemePalette BLU = new ThemePalette("blue", "Blu", 0xFF3D9BFF);
    public static final ThemePalette INDACO = new ThemePalette("indigo", "Indaco", 0xFF7A6CFF);
    public static final ThemePalette VIOLA = new ThemePalette("violet", "Viola", 0xFFBE5CFF);
    public static final ThemePalette MAGENTA = new ThemePalette("magenta", "Magenta", 0xFFFF4D93);
    public static final ThemePalette GHIACCIO = new ThemePalette("ice", "Ghiaccio", 0xFFE6EDF7);

    public static final ThemePalette[] ALL = {
            ROSSO, CORALLO, ARANCIO, AMBRA, VERDE, TURCHESE,
            BLU, INDACO, VIOLA, MAGENTA, GHIACCIO
    };

    public static ThemePalette byId(String id) {
        if (id != null) {
            for (ThemePalette p : ALL) {
                if (p.id.equals(id)) return p;
            }
        }
        return ROSSO;
    }

    public int withAlpha(int alpha) {
        return (accent & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    public int soft() {
        return withAlpha(0x26);
    }

    public int glowColor() {
        return withAlpha(0x55);
    }

    public static int blackText(int accent) {
        double luma = Color.red(accent) * 0.299 + Color.green(accent) * 0.587 + Color.blue(accent) * 0.114;
        return luma > 175 ? 0xFF10141A : 0xFFFFFFFF;
    }
}
