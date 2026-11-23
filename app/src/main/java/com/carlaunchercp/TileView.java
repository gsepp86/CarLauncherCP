package com.carlaunchercp;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Square shortcut card: Radio, Musica, second music app, Telefono and Navigatore. */
public class TileView extends FrameLayout {

    private final ThemePalette theme;
    private final ImageView iconView;
    private final TextView titleView;
    private final TextView subtitleView;

    public TileView(Context context, ThemePalette theme, boolean primary) {
        this(context, theme, primary, 0);
    }

    /**
     * @param accentColor colour of the icon plate; 0 keeps the theme accent. Each shortcut gets its
     *                    own colour so the row stays readable at a glance while driving.
     */
    public TileView(Context context, ThemePalette theme, boolean primary, int accentColor) {
        super(context);
        this.theme = theme;
        int tint = accentColor == 0 ? theme.accent : accentColor;

        setClickable(true);
        setFocusable(true);
        setBackground(background(primary));
        Ui.pressScale(this);

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);
        column.setPadding(Ui.dp(context, 6), Ui.dp(context, 5), Ui.dp(context, 6), Ui.dp(context, 5));

        FrameLayout iconBox = new FrameLayout(context);
        iconBox.setBackground(Ui.shape(Ui.alpha(tint, primary ? 0x38 : 0x22), 19f, context,
                Ui.alpha(tint, primary ? 0x80 : 0x6E), 1.5f));
        iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconBox.addView(iconView, new FrameLayout.LayoutParams(
                Ui.dp(context, 38), Ui.dp(context, 38), Gravity.CENTER));
        int box = Ui.dp(context, 56);
        column.addView(iconBox, new LinearLayout.LayoutParams(box, box));

        titleView = Ui.text(context, "", 19.5f,
                primary ? 0xFFFFFFFF : 0xFFF3F6FB, Ui.displayBold(context));
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        titleView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = Ui.dp(context, 5);
        column.addView(titleView, titleLp);

        subtitleView = Ui.text(context, "", 12.5f,
                primary ? 0xD9FFFFFF : 0xFF9AA4B4, Ui.medium(context));
        subtitleView.setSingleLine(true);
        subtitleView.setEllipsize(TextUtils.TruncateAt.END);
        subtitleView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        subLp.topMargin = Ui.dp(context, 1);
        column.addView(subtitleView, subLp);

        addView(column, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    private Drawable background(boolean primary) {
        Context c = getContext();
        if (primary) {
            return Ui.card(c, Ui.mix(theme.accent, 0xFFFFFFFF, 0.12f), Ui.darken(theme.accent, 0.28f),
                    22f, 0x40FFFFFF, 1f, 0x33FFFFFF);
        }
        return Ui.card(c, 0xFF161B23, 0xFF10141A, 22f, 0x26FFFFFF, theme.withAlpha(0x30));
    }

    public void setData(int iconRes, CharSequence title, CharSequence subtitle) {
        iconView.setImageResource(iconRes);
        iconView.setImageTintList(null);
        titleView.setText(title);
        subtitleView.setText(subtitle);
    }

    public void setSubtitle(CharSequence subtitle) {
        subtitleView.setText(subtitle);
    }
}
