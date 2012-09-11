package org.ebookdroid.common.bitmaps;

import org.ebookdroid.core.PagePaint;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;


public interface IBitmapRef {

    Canvas getCanvas();

    Bitmap getBitmap();

    void draw(final Canvas canvas, final PagePaint paint, final Rect src, final RectF r);

    void draw(Canvas canvas, int left, int top, Paint paint);

    void draw(Canvas canvas, Matrix matrix, Paint paint);

    void draw(final Canvas canvas, final Rect src, RectF dst, Paint p);

    void draw(final Canvas canvas, final Rect src, Rect dst, Paint p);

    void setPixels(int[] pixels, int width, int height);

    void setPixels(final RawBitmap raw);

    void getPixels(final RawBitmap slice, final int left, final int top, final int width, final int height);

    void getPixels(int[] pixels, int width, int height);

    void eraseColor(final int color);

    int getAverageColor();

    boolean isRecycled();

}
