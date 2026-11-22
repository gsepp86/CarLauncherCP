package com.carlaunchercp;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

/** Styling helpers shared by every screen of the launcher. */
public final class Ui {

    private static final Map<String, Typeface> FONT_CACHE = new HashMap<>();

    private Ui() {
    }

    public static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    public static Typeface font(Context c, String asset, int fallbackStyle) {
        Typeface cached = FONT_CACHE.get(asset);
        if (cached == null) {
            try {
                cached = Typeface.createFromAsset(c.getAssets(), asset);
            } catch (Exception e) {
                cached = Typeface.create("sans-serif-condensed", fallbackStyle);
            }
            FONT_CACHE.put(asset, cached);
        }
        return cached;
    }

    public static Typeface displayBold(Context c) {
        return font(c, "fonts/Rajdhani-Bold.ttf", Typeface.BOLD);
    }

    public static Typeface display(Context c) {
        return font(c, "fonts/Rajdhani-SemiBold.ttf", Typeface.BOLD);
    }

    public static Typeface medium(Context c) {
        return font(c, "fonts/Rajdhani-Medium.ttf", Typeface.NORMAL);
    }

    public static int alpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    public static int mix(int from, int to, float amount) {
        float t = Math.max(0f, Math.min(1f, amount));
        int a = Math.round(android.graphics.Color.alpha(from) + (android.graphics.Color.alpha(to) - android.graphics.Color.alpha(from)) * t);
        int r = Math.round(android.graphics.Color.red(from) + (android.graphics.Color.red(to) - android.graphics.Color.red(from)) * t);
        int g = Math.round(android.graphics.Color.green(from) + (android.graphics.Color.green(to) - android.graphics.Color.green(from)) * t);
        int b = Math.round(android.graphics.Color.blue(from) + (android.graphics.Color.blue(to) - android.graphics.Color.blue(from)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int darken(int color, float amount) {
        return mix(color, 0xFF05070A, amount);
    }

    public static GradientDrawable shape(int fill, float radiusDp, Context c, int strokeColor, float strokeWidthDp) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(fill);
        d.setCornerRadius(dp(c, radiusDp));
        if (strokeWidthDp > 0f) {
            d.setStroke(dp(c, strokeWidthDp), strokeColor);
        }
        return d;
    }

    public static GradientDrawable oval(int fill, Context c, int strokeColor, float strokeWidthDp) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(fill);
        if (strokeWidthDp > 0f) {
            d.setStroke(dp(c, strokeWidthDp), strokeColor);
        }
        return d;
    }

    public static Drawable card(Context c, int top, int bottom, float radiusDp, int strokeColor, int rippleColor) {
        GradientDrawable content = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bottom});
        content.setCornerRadius(dp(c, radiusDp));
        content.setStroke(dp(c, 1f), strokeColor);
        if (rippleColor == 0) return content;

        GradientDrawable mask = new GradientDrawable();
        mask.setShape(GradientDrawable.RECTANGLE);
        mask.setCornerRadius(dp(c, radiusDp));
        mask.setColor(0xFFFFFFFF);
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask);
    }

    public static TextView text(Context c, CharSequence value, float sizeSp, int color, Typeface typeface) {
        TextView view = new TextView(c);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        if (typeface != null) view.setTypeface(typeface);
        return view;
    }

    public static ImageView icon(Context c, int drawableRes, float sizeDp, int tint) {
        ImageView view = new ImageView(c);
        view.setImageResource(drawableRes);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (tint != 0) view.setImageTintList(ColorStateList.valueOf(tint));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp));
        view.setLayoutParams(lp);
        return view;
    }

    public static FrameLayout iconBox(Context c, int drawableRes, float boxDp, float iconDp,
                                      int fill, int stroke, int tint, float radiusDp) {
        FrameLayout box = new FrameLayout(c);
        box.setBackground(shape(fill, radiusDp, c, stroke, stroke != 0 ? 1f : 0f));
        ImageView iv = icon(c, drawableRes, iconDp, tint);
        box.addView(iv, new FrameLayout.LayoutParams(dp(c, iconDp), dp(c, iconDp), Gravity.CENTER));
        box.setLayoutParams(new LinearLayout.LayoutParams(dp(c, boxDp), dp(c, boxDp)));
        return box;
    }

    /** Squash-on-touch feedback that still lets the normal click listener fire. */
    public static void pressScale(final View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.975f).scaleY(0.975f).setDuration(90).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    public static LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    public static LinearLayout.LayoutParams lp(int width, int height, float weight) {
        return new LinearLayout.LayoutParams(width, height, weight);
    }
}
