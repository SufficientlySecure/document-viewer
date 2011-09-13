package org.ebookdroid.core.bitmaps;

import android.graphics.Bitmap;

import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicInteger;

public class BitmapRef {

    private static final AtomicInteger SEQ = new AtomicInteger();

    public final int id = SEQ.incrementAndGet();
    final int size;
    long gen;
    Bitmap bitmap;
    SoftReference<Bitmap> ref;

    BitmapRef(final Bitmap bitmap, final long generation) {
        this.bitmap = bitmap;
        this.ref = new SoftReference<Bitmap>(bitmap);
        this.size = BitmapManager.getBitmapBufferSize(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        this.gen = generation;
    }

    public Bitmap getBitmap() {
        final Bitmap bmp = bitmap != null ? bitmap : ref.get();
        if (bmp != null) {
            ref = new SoftReference<Bitmap>(bmp);
        }
        return bmp;
    }

    public void clearDirectRef() {
        bitmap = null;
    }

    void restoreDirectRef(Bitmap bmp, long generation) {
        bitmap = bmp;
        ref = new SoftReference<Bitmap>(bmp);
        gen = generation;
    }

    void recycle() {
        final Bitmap bmp = bitmap != null ? bitmap : ref.get();
        if (bmp != null && bmp.isRecycled()) {
            bmp.recycle();
        }
        bitmap = null;
        ref = new SoftReference<Bitmap>(null);
    }
}