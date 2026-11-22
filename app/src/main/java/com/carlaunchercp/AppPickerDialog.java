package com.carlaunchercp;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Searchable app list used to pick favourites, the radio app and the music app. */
public final class AppPickerDialog {

    public interface Callback {
        void onPicked(ComponentName component);
    }

    private AppPickerDialog() {
    }

    public static void show(final Activity activity, final ThemePalette theme, final String title,
                            final Callback callback) {
        final Dialog dialog = new Dialog(activity, R.style.CardDialog);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(activity, 20);
        root.setPadding(pad, Ui.dp(activity, 18), pad, pad);
        root.setBackground(Ui.card(activity, 0xFF10141A, 0xFF0B0E13, 26f,
                theme.withAlpha(0x4D), 0));

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = Ui.text(activity, title, 24f, 0xFFF4F7FB, Ui.displayBold(activity));
        header.addView(heading, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageView close = Ui.icon(activity, R.drawable.ic_close, 22f, 0xFF8892A0);
        FrameLayout closeBox = new FrameLayout(activity);
        closeBox.setBackground(Ui.shape(0x14FFFFFF, 999f, activity, 0, 0));
        closeBox.addView(close, new FrameLayout.LayoutParams(Ui.dp(activity, 22), Ui.dp(activity, 22), Gravity.CENTER));
        closeBox.setClickable(true);
        closeBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        header.addView(closeBox, new LinearLayout.LayoutParams(Ui.dp(activity, 44), Ui.dp(activity, 44)));
        root.addView(header);

        final EditText search = new EditText(activity);
        search.setHint(activity.getString(R.string.search_apps));
        search.setHintTextColor(0xFF7E8797);
        search.setTextColor(0xFFE8EDF6);
        search.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 19f);
        search.setSingleLine(true);
        search.setBackground(Ui.shape(0x0FFFFFFF, 14f, activity, 0x14FFFFFF, 1f));
        search.setPadding(Ui.dp(activity, 14), Ui.dp(activity, 11), Ui.dp(activity, 14), Ui.dp(activity, 11));
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchLp.topMargin = Ui.dp(activity, 12);
        root.addView(search, searchLp);

        final ListView list = new ListView(activity);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setSelector(new android.graphics.drawable.ColorDrawable(0x00000000));
        list.setBackgroundColor(0x00000000);
        list.setCacheColorHint(0x00000000);
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int listHeight = Math.min(Ui.dp(activity, 440), (int) (metrics.heightPixels * 0.56f));
        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, listHeight);
        listLp.topMargin = Ui.dp(activity, 10);
        root.addView(list, listLp);

        final AppAdapter adapter = new AppAdapter(activity, theme, dialog, callback);
        list.setAdapter(adapter);

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
            window.setGravity(Gravity.CENTER);
            window.setLayout(Math.min((int) (metrics.widthPixels * 0.92f), Ui.dp(activity, 640)),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        adapter.load();
    }

    private static final class AppItem {
        String label;
        String flattened;
        Drawable icon;
    }

    private static final class AppAdapter extends BaseAdapter {

        private final Activity activity;
        private final ThemePalette theme;
        private final Dialog dialog;
        private final Callback callback;
        private final List<AppItem> all = new ArrayList<>();
        private final List<AppItem> shown = new ArrayList<>();

        AppAdapter(Activity activity, ThemePalette theme, Dialog dialog, Callback callback) {
            this.activity = activity;
            this.theme = theme;
            this.dialog = dialog;
            this.callback = callback;
        }

        void load() {
            new Thread(new Runnable() {
                @Override
                public void run() {
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
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            all.clear();
                            all.addAll(loaded);
                            shown.clear();
                            shown.addAll(loaded);
                            notifyDataSetChanged();
                        }
                    });
                }
            }).start();
        }

        void filter(String query) {
            String needle = query.trim().toLowerCase();
            shown.clear();
            if (needle.isEmpty()) {
                shown.addAll(all);
            } else {
                for (AppItem item : all) {
                    if (item.label.toLowerCase().contains(needle)
                            || item.flattened.toLowerCase().contains(needle)) {
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
            LinearLayout row;
            Holder holder;
            if (convertView == null) {
                row = new LinearLayout(activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(Ui.dp(activity, 10), Ui.dp(activity, 10),
                        Ui.dp(activity, 10), Ui.dp(activity, 10));

                ImageView icon = new ImageView(activity);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                row.addView(icon, new LinearLayout.LayoutParams(Ui.dp(activity, 48), Ui.dp(activity, 48)));

                LinearLayout column = new LinearLayout(activity);
                column.setOrientation(LinearLayout.VERTICAL);
                TextView title = Ui.text(activity, "", 21f, 0xFFF1F5FA, Ui.medium(activity));
                TextView subtitle = Ui.text(activity, "", 13f, 0xFF858E9C, Ui.medium(activity));
                subtitle.setLetterSpacing(0.04f);
                column.addView(title);
                column.addView(subtitle);
                LinearLayout.LayoutParams columnLp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                columnLp.leftMargin = Ui.dp(activity, 14);
                row.addView(column, columnLp);

                holder = new Holder();
                holder.icon = icon;
                holder.title = title;
                holder.subtitle = subtitle;
                row.setTag(holder);
            } else {
                row = (LinearLayout) convertView;
                holder = (Holder) row.getTag();
            }

            AppItem item = getItem(position);
            holder.icon.setImageDrawable(item.icon);
            holder.title.setText(item.label);
            int slash = item.flattened.indexOf('/');
            holder.subtitle.setText(slash > 0 ? item.flattened.substring(0, slash) : item.flattened);

            row.setOnClickListener(v -> {
                dialog.dismiss();
                callback.onPicked(ComponentName.unflattenFromString(
                        AppAdapter.this.getItem(position).flattened));
            });
            row.setBackgroundColor(0x00000000);
            row.setOnTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        v.setBackgroundColor(theme.withAlpha(0x1F));
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        v.setBackgroundColor(0x00000000);
                        break;
                    default:
                        break;
                }
                return false;
            });
            return row;
        }
    }

    private static final class Holder {
        ImageView icon;
        TextView title;
        TextView subtitle;
    }
}
