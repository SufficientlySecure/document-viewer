package org.emdev.utils.textmarkup.image;

import android.graphics.Bitmap;
import android.graphics.RectF;


public interface IImageData {

    public RectF getImageRect(boolean inline);

    public Bitmap getBitmap();
}
