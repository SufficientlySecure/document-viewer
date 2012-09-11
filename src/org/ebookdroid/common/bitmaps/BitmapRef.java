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

import org.emdev.common.android.VMRuntimeHack;
import org.emdev.utils.MathUtils;

class BitmapRef extends AbstractBitmapRef {

    private Bitmap bitmap;
    private boolean hacked;

    BitmapRef(final Bitmap bitmap, final long generation) {
        super(bitmap.getConfig(), bitmap.hasAlpha(), bitmap.getWidth(), bitmap.getHeight(), generation);
        this.bitmap = bitmap;
        if (BitmapManager.useBitmapHack) {
            hacked = VMRuntimeHack.trackFree(size);
        }
    }

    public Canvas getCanvas() {
        return new Canvas(bitmap);
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void draw(final Canvas canvas, final PagePaint paint, final Rect src, final RectF r) {
        src.set(0, 0, this.width, this.height);
        RectF dst = MathUtils.round(r);
        Paint p = paint.bitmapPaint;
        draw(canvas, src, dst, p);
    }

    public void draw(final Canvas canvas, final Rect src, RectF dst, Paint p) {
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

    public void draw(final Canvas canvas, final Rect src, Rect dst, Paint p) {
        if (this.bitmap != null) {
            try {
                canvas.drawBitmap(this.bitmap, src, dst, p);
            } catch (final Throwable th) {
                LCTX.e("Unexpected error: ", th);
            }
        }
    }

    public void getPixels(final RawBitmap slice, final int left, final int top, final int width, final int height) {
        slice.width = width;
        slice.height = height;
        bitmap.getPixels(slice.pixels, 0, width, left, top, width, height);
    }

    public void getPixels(int[] pixels, int width, int height) {
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
    }

    public void setPixels(int[] pixels, int width, int height) {
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }

    public void setPixels(final RawBitmap raw) {
        bitmap.setPixels(raw.pixels, 0, raw.width, 0, 0, raw.width, raw.height);
    }

    public void eraseColor(final int color) {
        bitmap.eraseColor(color);
    }

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

    public boolean isRecycled() {
        if (bitmap != null) {
            if (!bitmap.isRecycled()) {
                return false;
            }
            if (hacked) {
                hacked = false;
                VMRuntimeHack.trackAlloc(size);
            }
            bitmap = null;
        }
        return true;
    }

    void recycle() {
        if (bitmap != null) {
            if (BitmapManager.useEarlyRecycling) {
                bitmap.recycle();
            }
            if (hacked) {
                hacked = false;
                VMRuntimeHack.trackAlloc(size);
            }
            bitmap = null;
        }
    }

    @Override
    public String toString() {
        return "BitmapRef [id=" + id + ", name=" + name + ", width=" + width + ", height=" + height + ", size=" + size
                + "]";
    }

    public void draw(Canvas canvas, int left, int top, Paint paint) {
        canvas.drawBitmap(bitmap, left, top, paint);
    }

    public void draw(Canvas canvas, Matrix matrix, Paint paint) {
        canvas.drawBitmap(bitmap, matrix, paint);
    }
}
