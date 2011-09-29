package org.ebookdroid.core.codec;

import org.ebookdroid.core.bitmaps.BitmapRef;

import android.graphics.RectF;

public interface CodecPage {

    int getWidth();

    int getHeight();

    BitmapRef renderBitmap(int width, int height, RectF pageSliceBounds);

    void recycle();

    boolean isRecycled();
}
