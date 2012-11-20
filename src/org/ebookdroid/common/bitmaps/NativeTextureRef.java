package org.ebookdroid.common.bitmaps;

import org.ebookdroid.core.PagePaint;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Canvas.EdgeType;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import java.nio.Buffer;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.emdev.utils.MatrixUtils;

public class NativeTextureRef extends AbstractBitmapRef {

    private static final ThreadLocal<RawBitmap> threadSlices = new ThreadLocal<RawBitmap>();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile int ref;

    NativeTextureRef(final boolean hasAlpha, final int width, final int height, final long generation) {
        super(Config.ARGB_8888, hasAlpha, width, height, generation);
    }

    @Override
    public Canvas getCanvas() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Bitmap getBitmap() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void draw(final Canvas canvas, final PagePaint paint, final Rect src, final RectF r) {
        lock.readLock().lock();
        try {
            if (ref != 0) {
                final Matrix m = MatrixUtils.get();
                m.reset();
                m.postTranslate(src.left, src.top);
                m.postScale(r.width() / src.width(), r.height() / src.height());
                m.postTranslate(r.left, r.top);
                draw(canvas, m, paint.bitmapPaint);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void draw(final Canvas canvas, final int left, final int top, final Paint paint) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void draw(final Canvas canvas, final Matrix matrix, final Paint paint) {
        lock.readLock().lock();
        try {
            if (ref != 0) {
                canvas.save();
                canvas.setMatrix(matrix);

                if (!canvas.quickReject(0, 0, width, height, EdgeType.BW)) {
                    RawBitmap slice = threadSlices.get();
                    if (slice == null || slice.pixels.length < width * height) {
                        slice = new RawBitmap(width, height, hasAlpha);
                        threadSlices.set(slice);
                    }
                    slice.hasAlpha = hasAlpha;
                    getPixels(slice.pixels, width, height);
                    canvas.drawBitmap(slice.pixels, 0, width, 0, 0, width, height, hasAlpha, paint);
                }
                canvas.restore();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void draw(final Canvas canvas, final Rect src, final RectF dst, final Paint p) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void draw(final Canvas canvas, final Rect src, final Rect dst, final Paint p) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setPixels(final int[] pixels, final int width, final int height) {
        lock.writeLock().lock();
        try {
            ref = nativeSetPixels(this.ref, this.width, this.height, pixels, width, height);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void setPixels(final RawBitmap raw) {
        lock.writeLock().lock();
        try {
            ref = nativeSetPixels(ref, width, height, raw.pixels, raw.width, raw.height);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void getPixels(final RawBitmap slice, final int left, final int top, final int width, final int height) {
        lock.readLock().lock();
        try {
            nativeGetRegionPixels(this.ref, this.width, this.height, slice.pixels, left, top, width, height);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void getPixels(final int[] pixels, final int width, final int height) {
        lock.readLock().lock();
        try {
            nativeGetPixels(ref, pixels, width, height);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void eraseColor(final int color) {
        lock.writeLock().lock();
        try {
            ref = nativeEraseColor(ref, color, width, height);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int getAverageColor() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isRecycled() {
        lock.readLock().lock();
        try {
            return ref == 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    void recycle() {
        lock.writeLock().lock();
        try {
            ref = nativeRecycle(ref);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static native int nativeSetPixels(int ref, int ownwidth, int ownheight, int[] pixels, int width, int height);

    private static native void nativeGetRegionPixels(int ref, int ownwidth, int ownheight, int[] pixels, int left,
            int top, int width, int height);

    private static native void nativeGetPixels(int ref, int[] pixels, int width, int height);

    private static native int nativeEraseColor(int ref, int color, int width, int height);

    private static native int nativeRecycle(int ref);

    @Override
    public void setPixels(Buffer pixels) {
        // TODO Auto-generated method stub

    }

}
