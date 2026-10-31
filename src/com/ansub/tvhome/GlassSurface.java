package com.ansub.tvhome;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Shared glass chrome with two reusable 64x64 textures (32 KiB of pixel storage). */
final class GlassSurface extends Drawable {
    private static final int SIZE = 64;
    private static final Bitmap REST = texture(0xA6323B46, 0xA62A333E);
    private static final Bitmap FOCUSED = texture(0xD04A5664, 0xCA303C4A);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final BitmapShader shader;
    private final Matrix matrix = new Matrix();
    private final Path outline = new Path();
    private final RectF area = new RectF();
    private final int edgeColor;
    private float[] corners = new float[8];

    static GlassSurface panel(float scale, boolean focused) {
        return new GlassSurface(scale, focused);
    }

    private GlassSurface(float scale, boolean focused) {
        shader = new BitmapShader(focused ? FOCUSED : REST, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        fill.setShader(shader);
        edgeColor = focused ? 0x858BC7FF : 0x24FFFFFF;
        edge.setColor(edgeColor);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(Math.max(1, Math.round(scale)));
    }

    private static Bitmap texture(int light, int dark) {
        Bitmap bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();
        paint.setShader(new LinearGradient(0, 0, SIZE, SIZE, light, dark, Shader.TileMode.CLAMP));
        new Canvas(bitmap).drawRect(0, 0, SIZE, SIZE, paint);
        return bitmap;
    }

    void setCornerRadii(float[] radii) {
        corners = radii.clone();
        updateOutline();
        invalidateSelf();
    }

    @Override protected void onBoundsChange(Rect bounds) {
        matrix.setRectToRect(new RectF(0, 0, SIZE, SIZE), new RectF(bounds), Matrix.ScaleToFit.FILL);
        shader.setLocalMatrix(matrix);
        updateOutline();
    }

    private void updateOutline() {
        area.set(getBounds());
        float inset = edge.getStrokeWidth() / 2f;
        area.inset(inset, inset);
        outline.rewind();
        outline.addRoundRect(area, corners, Path.Direction.CW);
    }

    @Override public void draw(Canvas canvas) {
        canvas.drawPath(outline, fill);
        canvas.drawPath(outline, edge);
    }

    @Override public void setAlpha(int alpha) {
        fill.setAlpha(alpha);
        edge.setAlpha(Color.alpha(edgeColor) * alpha / 255);
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter filter) {
        fill.setColorFilter(filter);
        edge.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
