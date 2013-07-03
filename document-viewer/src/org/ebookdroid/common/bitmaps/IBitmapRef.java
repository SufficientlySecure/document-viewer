package org.ebookdroid.common.bitmaps;

import android.graphics.Bitmap;
import android.graphics.Canvas;


public interface IBitmapRef {

    Canvas getCanvas();

    Bitmap getBitmap();

    boolean isRecycled();

}
