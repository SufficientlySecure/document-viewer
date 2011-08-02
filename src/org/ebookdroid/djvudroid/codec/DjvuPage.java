package org.ebookdroid.djvudroid.codec;

import org.ebookdroid.core.codec.CodecPage;

import android.graphics.Bitmap;
import android.graphics.RectF;

public class DjvuPage implements CodecPage {

    private long pageHandle;

    // TODO: remove all async operations

    DjvuPage(final long pageHandle) {
        this.pageHandle = pageHandle;
    }

    private static native int getWidth(long pageHandle);

    private static native int getHeight(long pageHandle);

    private static native boolean isDecodingDone(long pageHandle);

    private static native boolean renderPage(long pageHandle, int targetWidth, int targetHeight, float pageSliceX,
            float pageSliceY, float pageSliceWidth, float pageSliceHeight, int[] buffer);

    private static native void free(long pageHandle);

    @Override
    public int getWidth() {
        return getWidth(pageHandle);
    }

    @Override
    public int getHeight() {
        return getHeight(pageHandle);
    }

    @Override
    public Bitmap renderBitmap(final int width, final int height, final RectF pageSliceBounds) {
        final int[] buffer = new int[width * height];
        renderPage(pageHandle, width, height, pageSliceBounds.left, pageSliceBounds.top, pageSliceBounds.width(),
                pageSliceBounds.height(), buffer);
        return Bitmap.createBitmap(buffer, width, height, Bitmap.Config.RGB_565);
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    @Override
    public synchronized void recycle() {
        if (pageHandle == 0) {
            return;
        }
        free(pageHandle);
        pageHandle = 0;
    }

}
