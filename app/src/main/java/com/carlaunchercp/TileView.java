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

/** Large tappable shortcut card (Radio FM, Musica Bluetooth, Telefono). */
public class TileView extends LinearLayout {

    private final ThemePalette theme;
    private final ImageView iconView;
    private final TextView titleView;
    private final TextView subtitleView;

    /**
     * @param stacked when true the card stacks icon over text and stretches in height
     *                (landscape dashboard); otherwise icon and text sit side by side.
     */
    public TileView(Context context, ThemePalette theme, boolean primary, boolean stacked) {
        super(context);
        this.theme = theme;

        setOrientation(stacked ? VERTICAL : HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setClickable(true);
        setFocusable(true);
        int pad = Ui.dp(context, stacked ? 18 : 16);
        setPadding(pad, pad, pad, pad);
        setBackground(background(primary));
        Ui.pressScale(this);

        int boxSize = Ui.dp(context, stacked ? 72 : 54);
        FrameLayout box = new FrameLayout(context);
        box.setBackground(Ui.shape(primary ? 0x24FFFFFF : theme.withAlpha(0x26),
                stacked ? 20f : 18f, context,
                primary ? 0x33FFFFFF : theme.withAlpha(0x40), 1f));
        iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconView.setImageTintList(ColorStateList.valueOf(primary ? 0xFFFFFFFF : theme.accent));
        int iconSize = Ui.dp(context, stacked ? 34 : 27);
        box.addView(iconView, new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));
        addView(box, new LinearLayout.LayoutParams(boxSize, boxSize));

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(VERTICAL);
        titleView = Ui.text(context, "", stacked ? 21f : 17f,
                primary ? 0xFFFFFFFF : 0xFFF3F6FB, Ui.displayBold(context));
        titleView.setSingleLine(true);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        subtitleView = Ui.text(context, "", stacked ? 11f : 10.5f,
                primary ? 0xCCFFFFFF : 0xFF8A93A2, Ui.medium(context));
        subtitleView.setSingleLine(true);
        subtitleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        column.addView(titleView, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        subLp.topMargin = Ui.dp(context, 4);
        column.addView(subtitleView, subLp);

        if (stacked) {
            View spacer = new View(context);
            addView(spacer, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(context, 16)));
            addView(column, new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        } else {
            LinearLayout.LayoutParams columnLp = new LinearLayout.LayoutParams(0,
                    LayoutParams.WRAP_CONTENT, 1f);
            columnLp.leftMargin = Ui.dp(context, 14);
            addView(column, columnLp);
            addView(Ui.icon(context, R.drawable.ic_chevron, 15f,
                    primary ? 0x99FFFFFF : 0x3DFFFFFF));
        }
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
}
