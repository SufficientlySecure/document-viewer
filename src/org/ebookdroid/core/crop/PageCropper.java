package org.ebookdroid.core.crop;

import org.ebookdroid.common.bitmaps.ByteBufferBitmap;

import android.graphics.RectF;

import java.nio.ByteBuffer;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;

public class PageCropper {

    private static final LogContext LCTX = LogManager.root().lctx("PageCropper", false);

    public static final int BMP_SIZE = 400;

    private PageCropper() {
    }

    public static synchronized RectF getCropBounds(final ByteBufferBitmap bitmap, final RectF psb) {
        // LCTX.d("bitmap=" + bitmap.getWidth() + ", " + bitmap.getHeight() + ", slice=" + psb);
        RectF rect = nativeGetCropBounds(bitmap.getPixels(), bitmap.getWidth(), bitmap.getHeight(), psb.left, psb.top, psb.right, psb.bottom);
        // LCTX.d("cropped=" + rect);
        return rect;
    }

    public static synchronized RectF getColumn(ByteBufferBitmap bitmap, float x, float y) {
        return nativeGetColumn(bitmap.getPixels(), bitmap.getWidth(), bitmap.getHeight(), x, y);
    }

    private static native RectF nativeGetCropBounds(ByteBuffer pixels, int width, int height, float left, float top,
            float right, float bottom);

    private static native RectF nativeGetColumn(ByteBuffer pixels, int width, int height, float x, float y);
}
