package org.ebookdroid.djvudroid.codec;

import org.ebookdroid.core.bitmaps.BitmapManager;
import org.ebookdroid.core.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.settings.SettingsManager;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class DjvuPage implements CodecPage {

    private static final boolean useNativeGraphics;

    static {
        useNativeGraphics = isNativeGraphicsAvailable();
    }

    private long pageHandle;

    // TODO: remove all async operations

    DjvuPage(final long pageHandle) {
        this.pageHandle = pageHandle;
    }

    private static native int getWidth(long pageHandle);

    private static native int getHeight(long pageHandle);

    private static native boolean isDecodingDone(long pageHandle);

    private static native boolean renderPage(long pageHandle, int targetWidth, int targetHeight, float pageSliceX,
            float pageSliceY, float pageSliceWidth, float pageSliceHeight, int[] buffer, int renderMode);

    private static native boolean isNativeGraphicsAvailable();

    private static native boolean renderPageBitmap(long pageHandle, int targetWidth, int targetHeight,
            float pageSliceX, float pageSliceY, float pageSliceWidth, float pageSliceHeight, Bitmap bitmap,
            int renderMode);

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
    public BitmapRef renderBitmap(final int width, final int height, final RectF pageSliceBounds) {
        if (useNativeGraphics) {
            BitmapRef bmp = BitmapManager.getBitmap(width, height, Bitmap.Config.RGB_565);
            if (!renderPageBitmap(pageHandle, width, height, pageSliceBounds.left, pageSliceBounds.top,
                    pageSliceBounds.width(), pageSliceBounds.height(), bmp.getBitmap(), SettingsManager
                            .getAppSettings().getDjvuRenderingMode())) {
                Canvas c = new Canvas(bmp.getBitmap());
                Paint paint = new Paint();
                paint.setColor(Color.GRAY);
                c.drawRect(new Rect(0, 0, width, height), paint);
            }
            return bmp;
        }

        final int[] buffer = new int[width * height];
        renderPage(pageHandle, width, height, pageSliceBounds.left, pageSliceBounds.top, pageSliceBounds.width(),
                pageSliceBounds.height(), buffer, SettingsManager.getAppSettings().getDjvuRenderingMode());
        BitmapRef b = BitmapManager.getBitmap(width, height, Bitmap.Config.RGB_565);
        b.getBitmap().setPixels(buffer, 0, width, 0, 0, width, height);
        return b;
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

    @Override
    public synchronized boolean isRecycled() {
        return pageHandle == 0;
    }
}
