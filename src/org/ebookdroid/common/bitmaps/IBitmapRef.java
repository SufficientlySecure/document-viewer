package org.ebookdroid.common.bitmaps;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.nio.Buffer;


public interface IBitmapRef {

    Canvas getCanvas();

    Bitmap getBitmap();

    void draw(final Canvas canvas, final Rect src, Rect dst, Paint p);

    void setPixels(Buffer pixels);

    void getPixels(int[] pixels, int width, int height);

    void eraseColor(final int color);

    int getAverageColor();

    boolean isRecycled();

}
