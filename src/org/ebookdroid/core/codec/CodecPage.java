package org.ebookdroid.core.codec;

import android.graphics.Bitmap;
import android.graphics.RectF;

public interface CodecPage
{
    int getWidth();

    int getHeight();

    Bitmap renderBitmap(int width, int height, RectF pageSliceBounds);
    
    void recycle();
}
