package com.carlaunchercp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Small animated level bars: they dance next to the clock and date. */
public class EqualizerBarsView extends View {

    private static final int BARS = 4;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bar = new RectF();
    private int accent = 0xFFFFFFFF;
    private boolean running;
    private long startedAt = System.nanoTime();

    public EqualizerBarsView(Context context) {
        super(context);
        paint.setStyle(Paint.Style.FILL);
    }

    public void setAccent(int color) {
        accent = color;
        invalidate();
    }

    public void setRunning(boolean value) {
        if (running == value) return;
        running = value;
        startedAt = System.nanoTime();
        invalidate();
        if (running) postInvalidateOnAnimation();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startedAt = System.nanoTime();
        if (running) postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float gap = w * 0.16f;
        float barWidth = (w - gap * (BARS - 1)) / BARS;
        float radius = barWidth / 2f;
        double seconds = (System.nanoTime() - startedAt) / 1_000_000_000.0;

        for (int i = 0; i < BARS; i++) {
            float level;
            if (running) {
                double phase = seconds * 3.1 + i * 1.15;
                level = 0.34f + 0.62f * (0.5f + 0.5f * (float) Math.sin(phase));
            } else {
                level = 0.16f + 0.07f * i;
            }
            float barHeight = h * Math.min(1f, Math.max(0.14f, level));
            float left = i * (barWidth + gap);
            bar.set(left, h - barHeight, left + barWidth, h);
            paint.setColor(accent);
            paint.setAlpha(running ? (int) (0x99 + 0x66 * level) : 0x40);
            canvas.drawRoundRect(bar, radius, radius, paint);
        }

        if (running) postInvalidateOnAnimation();
    }
}
