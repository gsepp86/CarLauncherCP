package com.carlaunchercp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * Soft smoke haze drifting behind the header: bigger, brighter accent-tinted puffs that
 * rise from below the clock all the way to the top of the header, sway sideways and fade
 * out. Drawn from one cached blob bitmap, so the animation stays cheap on a car head unit.
 */
public class SmokeView extends View {

    private static final int WISPS = 9;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF dest = new RectF();
    private Bitmap puff;
    private ThemePalette theme;
    private long startedAt = System.nanoTime();

    public SmokeView(Context context) {
        super(context);
    }

    public void setTheme(ThemePalette palette) {
        if (theme == palette) return;
        theme = palette;
        rebuild();
        invalidate();
    }

    private void rebuild() {
        int size = Ui.dp(getContext(), 180);
        if (size <= 0) return;
        int tint = theme == null ? 0xFFFFFFFF : Ui.mix(theme.accent, 0xFFFFFFFF, 0.5f);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint blob = new Paint(Paint.ANTI_ALIAS_FLAG);
        float center = size * 0.5f;
        blob.setShader(new RadialGradient(center, center, center,
                new int[]{Ui.alpha(tint, 0x96), Ui.alpha(tint, 0x3D), Ui.alpha(tint, 0x00)},
                new float[]{0f, 0.52f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(center, center, center, blob);
        if (puff != null) puff.recycle();
        puff = bitmap;
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
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (puff == null || w <= 0 || h <= 0) return;

        float seconds = (System.nanoTime() - startedAt) / 1_000_000_000f;
        for (int i = 0; i < WISPS; i++) {
            float seed = i * 0.587f;
            float speed = 0.075f + 0.016f * (i % 3);
            float progress = (seconds * speed + seed) % 1f;
            float lane = i / (float) WISPS;
            float sway = (float) (Math.sin(seconds * 0.6f + i * 1.9f)
                    + 0.35f * Math.sin(seconds * 0.27f + i));
            float cx = w * (0.04f + lane * 0.92f) + sway * w * 0.07f;
            float cy = h * (1.06f - progress * 1.30f);
            float rx = h * (0.56f + progress * 0.84f);
            float ry = rx * 1.35f;
            float fade = (float) Math.sin(Math.PI * progress);
            int alpha = Math.round(fade * (0x5E + (i % 3) * 0x08));
            if (alpha <= 0) continue;

            paint.setAlpha(alpha);
            dest.set(cx - rx, cy - ry, cx + rx, cy + ry);
            canvas.drawBitmap(puff, null, dest, paint);
        }

        if (isShown()) postInvalidateOnAnimation();
    }
}
