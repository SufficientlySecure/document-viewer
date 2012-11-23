package org.ebookdroid.common.bitmaps;

import android.graphics.Bitmap;
import android.graphics.Canvas;

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
        bitmap = null;
    }

    @Override
    public String toString() {
        return "BitmapRef [id=" + id + ", name=" + name + ", width=" + width + ", height=" + height + ", size=" + size
                + "]";
    }
}
