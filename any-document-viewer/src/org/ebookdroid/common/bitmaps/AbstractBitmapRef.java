package org.ebookdroid.common.bitmaps;

import android.graphics.Bitmap;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.emdev.common.log.LogContext;

abstract class AbstractBitmapRef implements IBitmapRef {

    protected static final LogContext LCTX = BitmapManager.LCTX;

    private static final AtomicInteger SEQ = new AtomicInteger();

    public final int id = SEQ.incrementAndGet();
    public final int size;
    public final int width;
    public final int height;
    public final Bitmap.Config config;
    public final boolean hasAlpha;

    final AtomicBoolean used = new AtomicBoolean(true);

    long gen;
    String name;

    AbstractBitmapRef(final Bitmap.Config config, boolean hasAlpha, int width, int height, final long generation) {
        this.config = config;
        this.hasAlpha = hasAlpha;
        this.width = width;
        this.height = height;
        this.size = BitmapManager.getBitmapBufferSize(width, height, config);
        this.gen = generation;
    }

    @Override
    protected final void finalize() throws Throwable {
        recycle();
    }

    abstract void recycle();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id=" + id + ", name=" + name + ", width=" + width + ", height=" + height
                + ", size=" + size + "]";
    }

}
