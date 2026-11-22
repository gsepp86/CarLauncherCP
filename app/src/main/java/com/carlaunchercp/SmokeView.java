package com.carlaunchercp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * Thin accent-tinted streaks that drift upward behind the header: smaller than the old
 * smoke puffs, sharper-edged and clearer, with a couple of stacked layers so the top area
 * feels alive without dominating the clock. The body adds the same motion in its lower
 * strip, so the screen reads as one breathing surface.
 */
public class SmokeView extends View {

    private static final int LAYERS = 4;
    private static final int STREAKS_PER_LAYER = 5;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF lane = new RectF();
    private ThemePalette theme;
    private long startedAt = System.nanoTime();
    private float cornerDp;
    private boolean anchorBottom;

    public SmokeView(Context context) {
        super(context);
    }

    /** Streaks fade out towards the top, for the strip that sits at the bottom of the screen. */
    public void setAnchorBottom(boolean bottom) {
        anchorBottom = bottom;
        invalidate();
    }

    public void setTheme(ThemePalette palette) {
        if (theme == palette) return;
        theme = palette;
        cornerDp = Ui.dp(getContext(), 22f);
        invalidate();
    }

    private int accentTop() {
        return theme == null ? 0x96FFFFFF : Ui.alpha(theme.accent, 0x96);
    }

    private int accentMid() {
        return theme == null ? 0x3DFFFFFF : Ui.alpha(theme.accent, 0x3D);
    }

    private int accentEdge() {
        return theme == null ? 0x10FFFFFF : Ui.alpha(theme.accent, 0x10);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startedAt = System.nanoTime();
        postInvalidateOnAnimation();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) postInvalidateOnAnimation();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        path.reset();
        if (w > 0 && h > 0) {
            path.addRoundRect(0f, 0f, w, h, cornerDp, cornerDp, Path.Direction.CW);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float seconds = (System.nanoTime() - startedAt) / 1_000_000_000f;
        for (int layer = 0; layer < LAYERS; layer++) {
            float layerSpeed = 0.045f + layer * 0.010f;
            float layerSeed = layer * 0.73f + layer * layer * 0.21f;
            float layerRise = 0.55f + layer * 0.10f;

            fill.setAlpha(90 + layer * 22);
            fill.setShader(null);
            fill.setColor(accentTop());
            edge.setColor(accentEdge());
            edge.setStrokeWidth(Ui.dp(getContext(), 1.4f + layer * 0.18f));
            final int fillAlpha = 90 + layer * 22;
            final int edgeAlpha = accentEdge() >>> 24;

            for (int s = 0; s < STREAKS_PER_LAYER; s++) {
                float seed = layerSeed + s * 0.617f;
                float progress = (seconds * layerSpeed + seed) % 1f;
                float laneX = (s + 0.5f) / STREAKS_PER_LAYER;
                float drift = (float) (
                        Math.sin(seconds * 0.5f + layer * 2.1f + s)
                        + 0.45f * Math.sin(seconds * 0.22f + s * 1.3f));
                float cx = w * (0.03f + laneX * 0.94f) + drift * w * 0.05f;
                float baseY = h * (0.06f + layer * 0.16f);
                float y = baseY + (h * layerRise) * (1f - progress);
                float laneW = h * (0.9f + progress * 2.6f);
                float laneH = h * (0.05f + progress * 0.05f);

                lane.set(cx - laneW, y - laneH, cx + laneW, y + laneH);
                float rx = lane.width() * 0.5f;
                float ry = lane.height() * 0.5f;
                float r = Math.min(cornerDp, Math.min(rx, ry));
                path.reset();
                path.addRoundRect(lane, r, r, Path.Direction.CW);

                float centre = y / h;
                float fade = Math.min(1f, Math.max(0f, (anchorBottom ? centre : 1f - centre) * 1.7f));
                fill.setAlpha((int) Math.round(fillAlpha * fade
                        * (0.45f + 0.55f * (float) Math.sin(Math.PI * progress))));
                canvas.drawPath(path, fill);

                if (fade > 0.25f) {
                    edge.setAlpha(Math.round(edgeAlpha * fade));
                    canvas.drawPath(path, edge);
                }
            }
        }

        if (isShown()) postInvalidateOnAnimation();
    }
}
