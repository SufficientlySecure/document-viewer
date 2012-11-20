package org.ebookdroid.common.bitmaps;

import org.ebookdroid.core.PagePaint;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Canvas.EdgeType;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import java.nio.Buffer;

import org.emdev.utils.MathUtils;

class BitmapRef extends AbstractBitmapRef {

    private volatile Bitmap bitmap;

    BitmapRef(final Bitmap bitmap, final long generation) {
        super(bitmap.getConfig(), bitmap.hasAlpha(), bitmap.getWidth(), bitmap.getHeight(), generation);
        this.bitmap = bitmap;
    }

    @Override
    public Canvas getCanvas() {
        return new Canvas(bitmap);
    }

    @Override
    public Bitmap getBitmap() {
        return bitmap;
    }

    @Override
    public void draw(final Canvas canvas, final PagePaint paint, final Rect src, final RectF r) {
        src.set(0, 0, this.width, this.height);
        final RectF dst = MathUtils.round(r);
        final Paint p = paint.bitmapPaint;
        draw(canvas, src, dst, p);
    }

    @Override
    public void draw(final Canvas canvas, final Rect src, final RectF dst, final Paint p) {
        if (this.bitmap != null) {
            try {
                if (!canvas.quickReject(dst, EdgeType.BW)) {
                    canvas.drawBitmap(this.bitmap, src, dst, p);
                }
            } catch (final Throwable th) {
                LCTX.e("Unexpected error: ", th);
            }
        }
    }

    @Override
    public void draw(final Canvas canvas, final Rect src, final Rect dst, final Paint p) {
        if (this.bitmap != null) {
            try {
                canvas.drawBitmap(this.bitmap, src, dst, p);
            } catch (final Throwable th) {
                LCTX.e("Unexpected error: ", th);
            }
        }
    }

    @Override
    public void getPixels(final RawBitmap slice, final int left, final int top, final int width, final int height) {
        slice.width = width;
        slice.height = height;
        bitmap.getPixels(slice.pixels, 0, width, left, top, width, height);
    }

    @Override
    public void getPixels(final int[] pixels, final int width, final int height) {
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
    }

    @Override
    public void setPixels(final int[] pixels, final int width, final int height) {
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }

    @Override
    public void setPixels(final Buffer pixels) {
        bitmap.copyPixelsFromBuffer(pixels);
    }

    @Override
    public void setPixels(final RawBitmap raw) {
        bitmap.setPixels(raw.pixels, 0, raw.width, 0, 0, raw.width, raw.height);
    }

    @Override
    public void eraseColor(final int color) {
        bitmap.eraseColor(color);
    }

    @Override
    public int getAverageColor() {
        final int w = Math.min(bitmap.getWidth(), 7);
        final int h = Math.min(bitmap.getHeight(), 7);
        long r = 0, g = 0, b = 0;
        for (int i = 0; i < w; ++i) {
            for (int j = 0; j < h; ++j) {
                final int color = bitmap.getPixel(i, j);
                r += color & 0xFF0000;
                g += color & 0xFF00;
                b += color & 0xFF;
            }
        }
        r /= w * h;
        g /= w * h;
        b /= w * h;
        r >>= 16;
        g >>= 8;
        return Color.rgb((int) (r & 0xFF), (int) (g & 0xFF), (int) (b & 0xFF));
    }

    @Override
    public boolean isRecycled() {
        if (bitmap != null) {
            if (!bitmap.isRecycled()) {
                return false;
            }
            bitmap = null;
        }
        return true;
    }

    @Override
    void recycle() {
        final Bitmap b = bitmap;
        bitmap = null;
        if (b != null) {
            if (BitmapManager.useEarlyRecycling) {
                b.recycle();
            }
        }
    }

    @Override
    public String toString() {
        return "BitmapRef [id=" + id + ", name=" + name + ", width=" + width + ", height=" + height + ", size=" + size
                + "]";
    }

    @Override
    public void draw(final Canvas canvas, final int left, final int top, final Paint paint) {
        canvas.drawBitmap(bitmap, left, top, paint);
    }

    @Override
    public void draw(final Canvas canvas, final Matrix matrix, final Paint paint) {
        canvas.drawBitmap(bitmap, matrix, paint);
    }
}
