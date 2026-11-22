package com.carlaunchercp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Circular speed gauge: accent arc, tick marks, big digits and km/h unit. */
public class SpeedGaugeView extends View {

    private static final float START_ANGLE = 138f;
    private static final float SWEEP = 264f;
    private static final float SCALE_MAX = 180f;

    private ThemePalette theme = ThemePalette.ROSSO;
    private float target = 0f;
    private float shown = 0f;
    private boolean hasFix = false;
    private boolean dimmed = false;

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progress = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ticks = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint digits = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint unit = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();

    public SpeedGaugeView(Context context) {
        super(context);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeCap(Paint.Cap.ROUND);
        track.setColor(0x16FFFFFF);

        progress.setStyle(Paint.Style.STROKE);
        progress.setStrokeCap(Paint.Cap.ROUND);

        ticks.setStyle(Paint.Style.STROKE);
        ticks.setStrokeCap(Paint.Cap.ROUND);

        digits.setTextAlign(Paint.Align.CENTER);
        digits.setTypeface(Ui.displayBold(context));
        digits.setLetterSpacing(-0.02f);

        unit.setTextAlign(Paint.Align.CENTER);
        unit.setTypeface(Ui.display(context));
        unit.setLetterSpacing(0.16f);
    }

    public void setTheme(ThemePalette palette) {
        this.theme = palette;
        invalidate();
    }

    /** Speed in km/h. The gauge eases toward the value. */
    public void setValue(float kmh) {
        target = Math.max(0f, Math.min(kmh, SCALE_MAX * 1.15f));
        postInvalidateOnAnimation();
    }

    public void setHasFix(boolean fix) {
        if (hasFix != fix) {
            hasFix = fix;
            invalidate();
        }
    }

    public void setDimmed(boolean value) {
        if (dimmed != value) {
            dimmed = value;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float cx = w / 2f;
        float cy = h / 2f + Ui.dp(getContext(), 4);
        float radius = Math.min(w * 0.42f, h * 0.40f);
        float stroke = Math.max(Ui.dp(getContext(), 9), radius * 0.095f);

        float diff = target - shown;
        if (Math.abs(diff) > 0.2f) {
            shown += diff * 0.22f;
            postInvalidateOnAnimation();
        } else {
            shown = target;
        }

        track.setStrokeWidth(stroke);
        progress.setStrokeWidth(stroke);
        arcBounds.set(cx - radius, cy - radius, cx + radius, cy + radius);
        canvas.drawArc(arcBounds, START_ANGLE, SWEEP, false, track);

        float ratio = Math.min(1f, shown / SCALE_MAX);
        if (ratio > 0.002f) {
            progress.setColor(theme.accent);
            progress.setShadowLayer(Ui.dp(getContext(), 16), 0f, 0f, theme.glowColor());
            canvas.drawArc(arcBounds, START_ANGLE, SWEEP * ratio, false, progress);
            progress.clearShadowLayer();
        }

        // tick marks around the arc
        float tickInner = radius - stroke * 0.85f;
        float tickOuter = radius + stroke * 0.95f;
        ticks.setStrokeWidth(Math.max(2f, radius * 0.014f));
        for (int i = 0; i <= 10; i++) {
            double angle = Math.toRadians(START_ANGLE + SWEEP * i / 10f);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            float length = (i % 5 == 0) ? tickOuter : tickOuter - stroke * 0.28f;
            ticks.setColor(i <= Math.round(ratio * 10f) ? theme.withAlpha(0xAA) : 0x22FFFFFF);
            canvas.drawLine(cx + cos * tickInner, cy + sin * tickInner,
                    cx + cos * length, cy + sin * length, ticks);
        }

        // inner decoration ring
        ticks.setColor(0x0DFFFFFF);
        ticks.setStrokeWidth(Math.max(1f, radius * 0.012f));
        canvas.drawCircle(cx, cy, radius * 0.62f, ticks);

        String value = hasFix ? String.valueOf(Math.round(shown)) : "--";
        digits.setTextSize(radius * 0.72f);
        digits.setColor(dimmed ? 0xFF5C6473 : 0xFFF7FAFF);
        canvas.drawText(value, cx, cy + radius * 0.16f, digits);

        unit.setTextSize(radius * 0.19f);
        unit.setColor(theme.withAlpha(0xFF));
        canvas.drawText("KM/H", cx, cy + radius * 0.46f, unit);
    }
}
