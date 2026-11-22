package com.carlaunchercp;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Large tappable shortcut card (Radio FM, Musica Bluetooth, Telefono, Navigatore). */
public class TileView extends LinearLayout {

    private final ThemePalette theme;
    private final ImageView iconView;
    private final TextView titleView;
    private final TextView subtitleView;
    private final EqualizerBarsView bars;
    private final ImageView chevron;

    public TileView(Context context, ThemePalette theme, boolean primary) {
        super(context);
        this.theme = theme;

        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setClickable(true);
        setFocusable(true);
        setPadding(Ui.dp(context, 16), Ui.dp(context, 14), Ui.dp(context, 16), Ui.dp(context, 14));
        setBackground(background(primary));
        Ui.pressScale(this);

        int boxSize = Ui.dp(context, 62);
        FrameLayout box = new FrameLayout(context);
        box.setBackground(Ui.shape(primary ? 0x24FFFFFF : theme.withAlpha(0x26),
                20f, context, primary ? 0x33FFFFFF : theme.withAlpha(0x40), 1f));
        iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconView.setImageTintList(ColorStateList.valueOf(primary ? 0xFFFFFFFF : theme.accent));
        int iconSize = Ui.dp(context, 32);
        box.addView(iconView, new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));
        addView(box, new LinearLayout.LayoutParams(boxSize, boxSize));

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(VERTICAL);
        titleView = Ui.text(context, "", 20f,
                primary ? 0xFFFFFFFF : 0xFFF3F6FB, Ui.displayBold(context));
        titleView.setSingleLine(true);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        subtitleView = Ui.text(context, "", 14f,
                primary ? 0xD9FFFFFF : 0xFF9AA4B4, Ui.medium(context));
        subtitleView.setSingleLine(true);
        subtitleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        column.addView(titleView, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        subLp.topMargin = Ui.dp(context, 3);
        column.addView(subtitleView, subLp);

        LinearLayout.LayoutParams columnLp = new LinearLayout.LayoutParams(0,
                LayoutParams.WRAP_CONTENT, 1f);
        columnLp.leftMargin = Ui.dp(context, 12);
        addView(column, columnLp);

        FrameLayout indicator = new FrameLayout(context);
        bars = new EqualizerBarsView(context);
        bars.setAccent(primary ? 0xFFFFFFFF : theme.accent);
        bars.setVisibility(GONE);
        indicator.addView(bars, new FrameLayout.LayoutParams(
                Ui.dp(context, 32), Ui.dp(context, 26), Gravity.CENTER));
        chevron = Ui.icon(context, R.drawable.ic_chevron, 18f,
                primary ? 0x99FFFFFF : 0x3DFFFFFF);
        indicator.addView(chevron, new FrameLayout.LayoutParams(
                Ui.dp(context, 18), Ui.dp(context, 18), Gravity.CENTER));
        addView(indicator, new LinearLayout.LayoutParams(Ui.dp(context, 32), Ui.dp(context, 32)));
    }

    private Drawable background(boolean primary) {
        Context c = getContext();
        if (primary) {
            return Ui.card(c, Ui.mix(theme.accent, 0xFFFFFFFF, 0.12f), Ui.darken(theme.accent, 0.28f),
                    22f, 0x40FFFFFF, 0x33FFFFFF);
        }
        return Ui.card(c, 0xFF161B23, 0xFF10141A, 22f, 0x1FFFFFFF, theme.withAlpha(0x30));
    }

    public void setData(int iconRes, CharSequence title, CharSequence subtitle) {
        iconView.setImageResource(iconRes);
        titleView.setText(title);
        subtitleView.setText(subtitle);
    }

    public void setSubtitle(CharSequence subtitle) {
        subtitleView.setText(subtitle);
    }

    /** Swaps the chevron for animated level bars while a track is playing. */
    public void setPlaying(boolean playing) {
        bars.setRunning(playing);
        bars.setVisibility(playing ? VISIBLE : GONE);
        chevron.setVisibility(playing ? GONE : VISIBLE);
    }
}
