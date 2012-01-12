package org.ebookdroid.core.bitmaps;

import android.graphics.Bitmap;

import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicInteger;

public class BitmapRef {

    private static final AtomicInteger SEQ = new AtomicInteger();

    public final int id = SEQ.incrementAndGet();
    public final int size;
    public final int width;
    public final int height;

    long gen;
    String name;
    Bitmap bitmap;
    SoftReference<Bitmap> ref;

    BitmapRef(final Bitmap bitmap, final long generation) {
        this.bitmap = bitmap;
        this.ref = new SoftReference<Bitmap>(bitmap);
        this.width = bitmap.getWidth();
        this.height = bitmap.getHeight();
        this.size = BitmapManager.getBitmapBufferSize(width, height, bitmap.getConfig());
        this.gen = generation;
    }

    public Bitmap getBitmap() {
        final Bitmap bmp = bitmap != null ? bitmap : ref != null ? ref.get() : null;
        if (bmp != null) {
            ref = new SoftReference<Bitmap>(bmp);
        }
        return bmp;
    }

    public void clearDirectRef() {
        if (bitmap != null) {
            ref = new SoftReference<Bitmap>(bitmap);
            bitmap = null;
        }
    }

    boolean clearEmptyRef() {
        final Bitmap bmp = bitmap != null ? bitmap : ref != null ? ref.get() : null;
        if (bmp != null && !bmp.isRecycled()) {
            return false;
        }
        bitmap = null;
        ref = null;
        return true;
    }

    void restoreDirectRef(final Bitmap bmp, final long generation) {
        bitmap = bmp;
        ref = new SoftReference<Bitmap>(bmp);
        gen = generation;
    }

    void recycle() {
        final Bitmap bmp = bitmap != null ? bitmap : ref != null ? ref.get() : null;
        if (bmp != null) {
            bmp.recycle();
        }
        bitmap = null;
        ref = null;
    }

    @Override
    public String toString() {
        return "BitmapRef [id=" + id + ", name=" + name + ", width=" + width + ", height=" + height + ", size=" + size
                + "]";
    }

}
