package com.carlaunchercp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/** Deep automotive backdrop: gradient paint, accent light blooms and soft vignette. */
public class CarBackgroundView extends View {

    private ThemePalette theme;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CarBackgroundView(Context context) {
        super(context);
        line.setStrokeWidth(1f);
    }

    public void setTheme(ThemePalette palette) {
        this.theme = palette;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        paint.setShader(new LinearGradient(0f, 0f, w * 0.25f, h,
                new int[]{0xFF0A0C10, 0xFF121821, 0xFF06070A},
                new float[]{0f, 0.52f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);

        if (theme != null) {
            float spread = Math.max(w, h);
            paint.setShader(new RadialGradient(w * 0.06f, -h * 0.18f, spread * 0.95f,
                    new int[]{theme.withAlpha(0x52), theme.withAlpha(0x00)},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);

            paint.setShader(new RadialGradient(w * 0.98f, h * 1.06f, spread * 0.75f,
                    new int[]{theme.withAlpha(0x22), theme.withAlpha(0x00)},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
        }

        line.setColor(0x07FFFFFF);
        float step = Ui.dp(getContext(), 74);
        for (float x = -h; x < w + h; x += step) {
            canvas.drawLine(x, h, x + h, 0, line);
        }

        paint.setShader(new LinearGradient(0f, h * 0.5f, 0f, h,
                new int[]{0x00000000, 0x7A000000}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, h * 0.5f, w, h, paint);
        paint.setShader(null);
    }
}
