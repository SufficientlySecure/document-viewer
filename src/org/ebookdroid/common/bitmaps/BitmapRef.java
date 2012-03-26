package org.ebookdroid.common.bitmaps;

import android.graphics.Bitmap;

import java.util.concurrent.atomic.AtomicInteger;

import org.emdev.utils.android.VMRuntimeHack;

public class BitmapRef {

    private static final AtomicInteger SEQ = new AtomicInteger();

    public final int id = SEQ.incrementAndGet();
    public final int size;
    public final int width;
    public final int height;
    long gen;
    String name;
    Bitmap bitmap;
    boolean hacked;

    BitmapRef(final Bitmap bitmap, final long generation) {
        this.bitmap = bitmap;
        this.width = bitmap.getWidth();
        this.height = bitmap.getHeight();
        this.size = BitmapManager.getBitmapBufferSize(width, height, bitmap.getConfig());
        this.gen = generation;
        if (BitmapManager.useBitmapHack) {
            hacked = VMRuntimeHack.trackFree(size);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
    }

    public Bitmap getBitmap() {
        return bitmap;
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
}
