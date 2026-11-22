package com.carlaunchercp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * "All apps" launcher sheet: near-full-screen grid, two enlarged columns of app cells,
 * so every icon on the head unit stays a comfortable tap target.
 */
public final class AllAppsDialog {

    private AllAppsDialog() {
    }

    public static void show(final Activity activity, final ThemePalette theme, final String title) {
        final Dialog dialog = new Dialog(activity, R.style.CardDialog);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(activity, 22);
        root.setPadding(pad, Ui.dp(activity, 18), pad, Ui.dp(activity, 10));
        root.setBackground(Ui.card(activity, 0xFF10141A, 0xFF0B0E13, 26f,
                theme.withAlpha(0x4D), 0));

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = Ui.text(activity, title, 26f, 0xFFF4F7FB, Ui.displayBold(activity));
        header.addView(heading, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        FrameLayout closeBox = new FrameLayout(activity);
        closeBox.setBackground(Ui.shape(0x14FFFFFF, 999f, activity, 0, 0));
        closeBox.addView(Ui.icon(activity, R.drawable.ic_close, 24f, 0xFF8892A0),
                new FrameLayout.LayoutParams(Ui.dp(activity, 24), Ui.dp(activity, 24),
                        Gravity.CENTER));
        closeBox.setClickable(true);
        closeBox.setOnClickListener(v -> dialog.dismiss());
        header.addView(closeBox, new LinearLayout.LayoutParams(Ui.dp(activity, 48),
                Ui.dp(activity, 48)));
        root.addView(header);

        final EditText search = new EditText(activity);
        search.setHint(activity.getString(R.string.search_apps));
        search.setHintTextColor(0xFF7E8797);
        search.setTextColor(0xFFE8EDF6);
        search.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f);
        search.setSingleLine(true);
        search.setBackground(Ui.shape(0x0FFFFFFF, 14f, activity, 0x14FFFFFF, 1f));
        search.setPadding(Ui.dp(activity, 14), Ui.dp(activity, 11),
                Ui.dp(activity, 14), Ui.dp(activity, 11));
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchLp.topMargin = Ui.dp(activity, 12);
        root.addView(search, searchLp);

        final GridView grid = new GridView(activity);
        grid.setNumColumns(2);
        grid.setHorizontalSpacing(Ui.dp(activity, 12));
        grid.setVerticalSpacing(Ui.dp(activity, 12));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setSelector(new ColorDrawable(0x00000000));
        grid.setBackgroundColor(0x00000000);
        grid.setCacheColorHint(0x00000000);
        grid.setClipToPadding(false);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        gridLp.topMargin = Ui.dp(activity, 14);
        root.addView(grid, gridLp);

        final AppAdapter adapter = new AppAdapter(activity, theme, dialog);
        grid.setAdapter(adapter);

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        dialog.setContentView(root);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            window.setGravity(Gravity.CENTER);
            window.setLayout((int) (metrics.widthPixels * 0.97f),
                    (int) (metrics.heightPixels * 0.95f));
        }
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0x00000000));

        adapter.load();
    }

    private static final class AppItem {
        String label;
        String flattened;
        Drawable icon;
    }

    private static String normalize(String text) {
        String decomposed = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD);
        StringBuilder out = new StringBuilder(decomposed.length());
        for (int i = 0; i < decomposed.length(); i++) {
            char c = decomposed.charAt(i);
            if (Character.getType(c) != Character.NON_SPACING_MARK) out.append(c);
        }
        return out.toString().toLowerCase(Locale.ROOT);
    }

    private static final class AppAdapter extends BaseAdapter {

        private final Activity activity;
        private final ThemePalette theme;
        private final Dialog dialog;
        private final List<AppItem> all = new ArrayList<>();
        private final List<AppItem> shown = new ArrayList<>();

        AppAdapter(Activity activity, ThemePalette theme, Dialog dialog) {
            this.activity = activity;
            this.theme = theme;
            this.dialog = dialog;
        }

        void load() {
            new Thread(() -> {
                PackageManager pm = activity.getPackageManager();
                Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> resolved = pm.queryIntentActivities(main, 0);
                final List<AppItem> loaded = new ArrayList<>();
                for (ResolveInfo info : resolved) {
                    if (info.activityInfo == null) continue;
                    String pkg = info.activityInfo.packageName;
                    if (pkg.equals(activity.getPackageName())) continue;
                    AppItem item = new AppItem();
                    item.label = info.loadLabel(pm).toString();
                    item.flattened = pkg + "/" + info.activityInfo.name;
                    item.icon = info.loadIcon(pm);
                    loaded.add(item);
                }
                Collections.sort(loaded, (a, b) -> a.label.compareToIgnoreCase(b.label));
                activity.runOnUiThread(() -> {
                    all.clear();
                    all.addAll(loaded);
                    shown.clear();
                    shown.addAll(loaded);
                    notifyDataSetChanged();
                });
            }).start();
        }

        void filter(String query) {
            String needle = normalize(query).trim();
            shown.clear();
            if (needle.isEmpty()) {
                shown.addAll(all);
            } else {
                for (AppItem item : all) {
                    if (normalize(item.label).contains(needle)
                            || normalize(item.flattened).contains(needle)) {
                        shown.add(item);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return shown.size();
        }

        @Override
        public AppItem getItem(int position) {
            return shown.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout cell;
            Holder holder;
            if (convertView == null) {
                cell = new LinearLayout(activity);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                cell.setPadding(Ui.dp(activity, 14), Ui.dp(activity, 16),
                        Ui.dp(activity, 14), Ui.dp(activity, 14));
                cell.setMinimumHeight(Ui.dp(activity, 138));
                cell.setBackground(Ui.card(activity, 0x12FFFFFF, 0x0AFFFFFF, 18f,
                        0x1FFFFFFF, theme.withAlpha(0x33)));

                FrameLayout iconBox = new FrameLayout(activity);
                iconBox.setBackground(Ui.shape(0xFF1A2029, 20f, activity, 0x1FFFFFFF, 1f));
                ImageView icon = new ImageView(activity);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                iconBox.addView(icon, new FrameLayout.LayoutParams(
                        Ui.dp(activity, 54), Ui.dp(activity, 54), Gravity.CENTER));
                cell.addView(iconBox, new LinearLayout.LayoutParams(
                        Ui.dp(activity, 76), Ui.dp(activity, 76)));

                TextView label = Ui.text(activity, "", 17f, 0xFFF1F5FA, Ui.medium(activity));
                label.setGravity(Gravity.CENTER);
                label.setSingleLine(true);
                label.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                labelLp.topMargin = Ui.dp(activity, 8);
                cell.addView(label, labelLp);

                holder = new Holder();
                holder.icon = icon;
                holder.label = label;
                cell.setTag(holder);
            } else {
                cell = (LinearLayout) convertView;
                holder = (Holder) cell.getTag();
            }

            AppItem item = getItem(position);
            holder.icon.setImageDrawable(item.icon);
            holder.label.setText(item.label);

            final String flattened = item.flattened;
            cell.setOnClickListener(v -> {
                dialog.dismiss();
                LauncherActions.launchComponent(activity, flattened);
            });
            Ui.pressScale(cell);
            return cell;
        }
    }

    private static final class Holder {
        ImageView icon;
        TextView label;
    }
}
