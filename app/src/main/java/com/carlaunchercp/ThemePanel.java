package com.carlaunchercp;

import android.app.Activity;
import android.app.Dialog;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Settings sheet: colour swatches, language and the apps bound to the tiles. */
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
        root.setPadding(Ui.dp(activity, 22), Ui.dp(activity, 18), Ui.dp(activity, 22), Ui.dp(activity, 16));
        root.setBackground(Ui.card(activity, 0xFF10141A, 0xFF0B0E13, 26f,
                current.withAlpha(0x59), 0));

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleBlock = new LinearLayout(activity);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        TextView title = Ui.text(activity, activity.getString(R.string.theme_title), 26f,
                0xFFF4F7FB, Ui.displayBold(activity));
        TextView subtitle = Ui.text(activity, activity.getString(R.string.theme_sub), 15f,
                0xFF8B94A2, Ui.medium(activity));
        titleBlock.addView(title);
        titleBlock.addView(subtitle);
        header.addView(titleBlock, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView close = Ui.icon(activity, R.drawable.ic_close, 22f, 0xFF8892A0);
        LinearLayout closeBox = new LinearLayout(activity);
        closeBox.setBackground(Ui.shape(0x14FFFFFF, 999f, activity, 0, 0));
        closeBox.setGravity(Gravity.CENTER);
        closeBox.addView(close);
        closeBox.setClickable(true);
        closeBox.setOnClickListener(v -> dialog.dismiss());
        header.addView(closeBox, new LinearLayout.LayoutParams(Ui.dp(activity, 46), Ui.dp(activity, 46)));
        root.addView(header);

        GridLayout grid = new GridLayout(activity);
        grid.setColumnCount(5);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridLp.topMargin = Ui.dp(activity, 4);
        root.addView(grid, gridLp);

        for (final ThemePalette palette : ThemePalette.ALL) {
            boolean selected = palette.id.equals(current.id);
            LinearLayout cell = new LinearLayout(activity);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(0, Ui.dp(activity, 4), 0, Ui.dp(activity, 2));

            LinearLayout halo = new LinearLayout(activity);
            halo.setGravity(Gravity.CENTER);
            halo.setBackground(Ui.oval(selected ? palette.withAlpha(0x22) : 0x00000000, activity,
                    selected ? palette.accent : 0x00000000, selected ? 1.5f : 0f));
            View swatch = new View(activity);
            swatch.setBackground(Ui.oval(palette.accent, activity, 0, 0));
            halo.addView(swatch, new LinearLayout.LayoutParams(Ui.dp(activity, 26), Ui.dp(activity, 26)));
            cell.addView(halo, new LinearLayout.LayoutParams(Ui.dp(activity, 46), Ui.dp(activity, 46)));

            TextView label = Ui.text(activity, palette.label(activity), 12.5f,
                    selected ? 0xFFE9EEF7 : 0xFF8B94A2, Ui.medium(activity));
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            labelLp.topMargin = Ui.dp(activity, 3);
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
            cellLp.setMargins(Ui.dp(activity, 2), 0, Ui.dp(activity, 2), 0);
            grid.addView(cell, cellLp);
        }

        root.addView(divider(activity));

        boolean english = LocaleHelper.EN.equals(prefs.language());
        LinearLayout languageRow = new LinearLayout(activity);
        languageRow.setOrientation(LinearLayout.HORIZONTAL);
        languageRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView languageTitle = Ui.text(activity, activity.getString(R.string.language_title), 18f,
                0xFFE6EBF4, Ui.medium(activity));
        languageRow.addView(languageTitle, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        languageRow.addView(pill(activity, current, activity.getString(R.string.language_italian),
                !english, () -> applyLanguage(activity, prefs, dialog, LocaleHelper.IT)));
        LinearLayout.LayoutParams englishLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        englishLp.leftMargin = Ui.dp(activity, 8);
        languageRow.addView(pill(activity, current, activity.getString(R.string.language_english),
                english, () -> applyLanguage(activity, prefs, dialog, LocaleHelper.EN)), englishLp);
        root.addView(languageRow);

        root.addView(divider(activity));

        TextView section = Ui.text(activity, activity.getString(R.string.apps_section), 14f,
                0xFF8B94A2, Ui.displayBold(activity));
        section.setLetterSpacing(0.2f);
        LinearLayout.LayoutParams sectionLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sectionLp.topMargin = Ui.dp(activity, 4);
        root.addView(section, sectionLp);

        final TextView radioValue = Ui.text(activity,
                valueFor(activity, prefs.radioTarget(), activity.getString(R.string.value_auto)), 16f,
                0xFFB6BECC, Ui.medium(activity));
        root.addView(row(activity, current, R.drawable.ic_radio,
                activity.getString(R.string.app_radio_row), radioValue,
                v -> AppPickerDialog.show(activity, current, activity.getString(R.string.choose_radio),
                        component -> {
                            prefs.setRadioTarget(component.flattenToString());
                            radioValue.setText(valueFor(activity, prefs.radioTarget(),
                                    activity.getString(R.string.value_auto)));
                        })));

        final TextView musicValue = Ui.text(activity,
                valueFor(activity, prefs.musicTarget(), activity.getString(R.string.value_auto)), 16f,
                0xFFB6BECC, Ui.medium(activity));
        root.addView(row(activity, current, R.drawable.ic_bluetooth,
                activity.getString(R.string.app_music_row), musicValue,
                v -> AppPickerDialog.show(activity, current, activity.getString(R.string.choose_music),
                        component -> {
                            prefs.setMusicTarget(component.flattenToString());
                            musicValue.setText(valueFor(activity, prefs.musicTarget(),
                                    activity.getString(R.string.value_auto)));
                        })));

        final TextView music2Value = Ui.text(activity,
                valueFor(activity, prefs.musicTarget2(), activity.getString(R.string.value_to_choose)),
                16f, 0xFFB6BECC, Ui.medium(activity));
        root.addView(row(activity, current, R.drawable.ic_music,
                activity.getString(R.string.app_music2_row), music2Value,
                v -> AppPickerDialog.show(activity, current, activity.getString(R.string.choose_music_2),
                        component -> {
                            prefs.setMusicTarget2(component.flattenToString());
                            music2Value.setText(valueFor(activity, prefs.musicTarget2(),
                                    activity.getString(R.string.value_to_choose)));
                        })));

        final TextView navValue = Ui.text(activity,
                valueFor(activity, prefs.navTarget(), activity.getString(R.string.value_to_choose)),
                16f, 0xFFB6BECC, Ui.medium(activity));
        root.addView(row(activity, current, R.drawable.ic_navigation,
                activity.getString(R.string.app_nav_row), navValue,
                v -> AppPickerDialog.show(activity, current, activity.getString(R.string.choose_nav),
                        component -> {
                            prefs.setNavTarget(component.flattenToString());
                            navValue.setText(valueFor(activity, prefs.navTarget(),
                                    activity.getString(R.string.value_to_choose)));
                        })));

        TextView hint = Ui.text(activity, activity.getString(R.string.theme_hint),
                13.5f, 0xFF7C8492, Ui.medium(activity));
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = Ui.dp(activity, 10);
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
            window.setLayout(Math.min((int) (metrics.widthPixels * 0.94f), Ui.dp(activity, 660)),
                    Math.min((int) (metrics.heightPixels * 0.94f), Ui.dp(activity, 600)));
        }
    }

    private static void applyLanguage(Activity activity, Prefs prefs, Dialog dialog, String tag) {
        dialog.dismiss();
        if (tag.equals(prefs.language())) return;
        prefs.setLanguage(tag);
        activity.recreate();
    }

    private static View pill(Activity activity, ThemePalette theme, String text,
                             boolean selected, Runnable onClick) {
        TextView pill = Ui.text(activity, text, 16f,
                selected ? ThemePalette.blackText(theme.accent) : 0xFFC3CBD8,
                Ui.displayBold(activity));
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(Ui.dp(activity, 18), Ui.dp(activity, 8),
                Ui.dp(activity, 18), Ui.dp(activity, 8));
        pill.setBackground(selected
                ? Ui.shape(theme.accent, 999f, activity, 0, 0)
                : Ui.shape(0x14FFFFFF, 999f, activity, 0x1FFFFFFF, 1f));
        pill.setClickable(true);
        pill.setOnClickListener(v -> onClick.run());
        return pill;
    }

    private static String valueFor(Activity activity, String flattened, String fallback) {
        if (flattened == null) return fallback;
        String label = LauncherActions.appLabel(activity, flattened);
        return label.isEmpty() ? fallback : label;
    }

    private static View divider(Activity activity) {
        View line = new View(activity);
        line.setBackgroundColor(0x14FFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(activity, 0.8f)));
        lp.topMargin = Ui.dp(activity, 12);
        lp.bottomMargin = Ui.dp(activity, 4);
        line.setLayoutParams(lp);
        return line;
    }

    private static View row(final Activity activity, final ThemePalette theme, int iconRes,
                            String label, TextView value, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(activity, 7), 0, Ui.dp(activity, 7));
        row.setClickable(true);

        row.addView(Ui.iconBox(activity, iconRes, 42f, 24f, 0xFF1A2029,
                0x1FFFFFFF, 0, 12f));

        TextView title = Ui.text(activity, label, 18f, 0xFFE6EBF4, Ui.medium(activity));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleLp.leftMargin = Ui.dp(activity, 12);
        row.addView(title, titleLp);
        row.addView(value);

        ImageView chevron = Ui.icon(activity, R.drawable.ic_chevron, 16f, 0x4DFFFFFF);
        LinearLayout.LayoutParams chevronLp = new LinearLayout.LayoutParams(
                Ui.dp(activity, 16), Ui.dp(activity, 16));
        chevronLp.leftMargin = Ui.dp(activity, 8);
        row.addView(chevron, chevronLp);

        row.setOnClickListener(onClick);
        return row;
    }
}
