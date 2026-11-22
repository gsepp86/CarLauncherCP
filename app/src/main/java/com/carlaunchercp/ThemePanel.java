package com.carlaunchercp;

import android.app.Activity;
import android.app.Dialog;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Bottom of the theme sheet: colour swatches plus the apps bound to the tiles. */
public final class ThemePanel {

    public interface Listener {
        void onThemeSelected(ThemePalette palette);
    }

    private ThemePanel() {
    }

    public static void show(final Activity activity, final ThemePalette current,
                            final Listener listener) {
        final Prefs prefs = new Prefs(activity);
        final Dialog dialog = new Dialog(activity, R.style.CardDialog);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(activity, 22), Ui.dp(activity, 20), Ui.dp(activity, 22), Ui.dp(activity, 20));
        root.setBackground(Ui.card(activity, 0xFF10141A, 0xFF0B0E13, 26f,
                current.withAlpha(0x59), 0));

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleBlock = new LinearLayout(activity);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        TextView title = Ui.text(activity, "Tema colore", 18f, 0xFFF4F7FB, Ui.displayBold(activity));
        TextView subtitle = Ui.text(activity, "Scegli l'accento dell'abitacolo", 10.5f,
                0xFF7F8896, Ui.medium(activity));
        titleBlock.addView(title);
        titleBlock.addView(subtitle);
        header.addView(titleBlock, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView close = Ui.icon(activity, R.drawable.ic_close, 16f, 0xFF8892A0);
        LinearLayout closeBox = new LinearLayout(activity);
        closeBox.setBackground(Ui.shape(0x14FFFFFF, 999f, activity, 0, 0));
        closeBox.setGravity(Gravity.CENTER);
        closeBox.addView(close);
        closeBox.setClickable(true);
        closeBox.setOnClickListener(v -> dialog.dismiss());
        header.addView(closeBox, new LinearLayout.LayoutParams(Ui.dp(activity, 34), Ui.dp(activity, 34)));
        root.addView(header);

        GridLayout grid = new GridLayout(activity);
        grid.setColumnCount(5);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridLp.topMargin = Ui.dp(activity, 6);
        root.addView(grid, gridLp);

        for (final ThemePalette palette : ThemePalette.ALL) {
            boolean selected = palette.id.equals(current.id);
            LinearLayout cell = new LinearLayout(activity);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(0, Ui.dp(activity, 8), 0, Ui.dp(activity, 6));

            LinearLayout halo = new LinearLayout(activity);
            halo.setGravity(Gravity.CENTER);
            halo.setBackground(Ui.oval(selected ? palette.withAlpha(0x22) : 0x00000000, activity,
                    selected ? palette.accent : 0x00000000, selected ? 1.5f : 0f));
            View swatch = new View(activity);
            swatch.setBackground(Ui.oval(palette.accent, activity, 0, 0));
            halo.addView(swatch, new LinearLayout.LayoutParams(Ui.dp(activity, 26), Ui.dp(activity, 26)));
            cell.addView(halo, new LinearLayout.LayoutParams(Ui.dp(activity, 42), Ui.dp(activity, 42)));

            TextView label = Ui.text(activity, palette.label, 9.5f,
                    selected ? 0xFFE9EEF7 : 0xFF8B94A2, Ui.medium(activity));
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            labelLp.topMargin = Ui.dp(activity, 4);
            cell.addView(label, labelLp);

            cell.setClickable(true);
            cell.setOnClickListener(v -> {
                listener.onThemeSelected(palette);
                dialog.dismiss();
            });

            GridLayout.LayoutParams cellLp = new GridLayout.LayoutParams();
            cellLp.width = 0;
            cellLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            cellLp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            cellLp.setMargins(Ui.dp(activity, 2), Ui.dp(activity, 2), Ui.dp(activity, 2), Ui.dp(activity, 2));
            grid.addView(cell, cellLp);
        }

        root.addView(divider(activity));

        TextView section = Ui.text(activity, "APP COLLEGATE", 9.5f, 0xFF7F8896, Ui.displayBold(activity));
        section.setLetterSpacing(0.2f);
        LinearLayout.LayoutParams sectionLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sectionLp.topMargin = Ui.dp(activity, 4);
        root.addView(section, sectionLp);

        final TextView radioValue = Ui.text(activity, valueFor(activity, prefs.radioTarget()), 11f,
                0xFFB6BECC, Ui.medium(activity));
        root.addView(row(activity, current, R.drawable.ic_radio, "App radio FM", radioValue,
                v -> AppPickerDialog.show(activity, current, "Scegli l'app radio", component -> {
                    prefs.setRadioTarget(component.flattenToString());
                    radioValue.setText(valueFor(activity, prefs.radioTarget()));
                })));

        final TextView musicValue = Ui.text(activity, valueFor(activity, prefs.musicTarget()), 11f,
                0xFFB6BECC, Ui.medium(activity));
        root.addView(row(activity, current, R.drawable.ic_bluetooth, "App musica", musicValue,
                v -> AppPickerDialog.show(activity, current, "Scegli l'app musica", component -> {
                    prefs.setMusicTarget(component.flattenToString());
                    musicValue.setText(valueFor(activity, prefs.musicTarget()));
                })));

        TextView hint = Ui.text(activity, "Il tema si applica subito. Tieni premuta un'app in evidenza per sostituirla.",
                9.5f, 0xFF6C7482, Ui.medium(activity));
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = Ui.dp(activity, 12);
        root.addView(hint, hintLp);

        ScrollView scroll = new ScrollView(activity);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dialog.setContentView(scroll, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            window.setGravity(Gravity.CENTER);
            window.setLayout(Math.min((int) (metrics.widthPixels * 0.94f), Ui.dp(activity, 540)),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private static String valueFor(Activity activity, String flattened) {
        if (flattened == null) return "Automatica";
        String label = LauncherActions.appLabel(activity, flattened);
        return label.isEmpty() ? "Automatica" : label;
    }

    private static View divider(Activity activity) {
        View line = new View(activity);
        line.setBackgroundColor(0x14FFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(activity, 0.8f)));
        lp.topMargin = Ui.dp(activity, 14);
        lp.bottomMargin = Ui.dp(activity, 4);
        line.setLayoutParams(lp);
        return line;
    }

    private static View row(final Activity activity, final ThemePalette theme, int iconRes,
                            String label, TextView value, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(activity, 10), 0, Ui.dp(activity, 10));
        row.setClickable(true);

        row.addView(Ui.iconBox(activity, iconRes, 34f, 17f, theme.withAlpha(0x1F),
                theme.withAlpha(0x33), theme.accent, 11f));

        TextView title = Ui.text(activity, label, 12.5f, 0xFFE6EBF4, Ui.medium(activity));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleLp.leftMargin = Ui.dp(activity, 12);
        row.addView(title, titleLp);
        row.addView(value);

        ImageView chevron = Ui.icon(activity, R.drawable.ic_chevron, 13f, 0x4DFFFFFF);
        LinearLayout.LayoutParams chevronLp = new LinearLayout.LayoutParams(
                Ui.dp(activity, 13), Ui.dp(activity, 13));
        chevronLp.leftMargin = Ui.dp(activity, 8);
        row.addView(chevron, chevronLp);

        row.setOnClickListener(onClick);
        return row;
    }
}
