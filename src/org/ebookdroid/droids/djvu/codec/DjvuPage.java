package org.ebookdroid.droids.djvu.codec;

import org.ebookdroid.EBookDroidLibraryLoader;
import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.PageLink;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DjvuPage implements CodecPage {

    private long docHandle;
    private long pageHandle;
    private int pageNo;

    // TODO: remove all async operations

    DjvuPage(final long docHandle, final long pageHandle, final int pageNo) {
        this.docHandle = docHandle;
        this.pageHandle = pageHandle;
        this.pageNo = pageNo;
    }

    private static native int getWidth(long pageHandle);

    private static native int getHeight(long pageHandle);

    private static native boolean isDecodingDone(long pageHandle);

    private static native boolean renderPage(long pageHandle, int targetWidth, int targetHeight, float pageSliceX,
            float pageSliceY, float pageSliceWidth, float pageSliceHeight, int[] buffer, int renderMode);

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
        final int renderMode = SettingsManager.getAppSettings().djvuRenderingMode;
        BitmapRef bmp = null;
        if (width > 0 && height > 0) {
            bmp = BitmapManager.getBitmap("Djvu page", width, height, Bitmap.Config.RGB_565);
            if (EBookDroidLibraryLoader.nativeGraphicsAvailable && SettingsManager.getAppSettings().useNativeGraphics) {
                if (renderPageBitmap(pageHandle, width, height, pageSliceBounds.left, pageSliceBounds.top,
                        pageSliceBounds.width(), pageSliceBounds.height(), bmp.getBitmap(), renderMode)) {
                    return bmp;
                }
            } else {
                final int[] buffer = new int[width * height];
                renderPage(pageHandle, width, height, pageSliceBounds.left, pageSliceBounds.top,
                        pageSliceBounds.width(), pageSliceBounds.height(), buffer, renderMode);
                bmp.getBitmap().setPixels(buffer, 0, width, 0, 0, width, height);
                return bmp;
            }
        }
        if (bmp == null) {
            bmp = BitmapManager.getBitmap("Djvu page", 100, 100, Bitmap.Config.RGB_565);
        }
        final Canvas c = new Canvas(bmp.getBitmap());
        final Paint paint = new Paint();
        paint.setColor(Color.GRAY);
        c.drawRect(new Rect(0, 0, width, height), paint);
        return bmp;
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

    @Override
    public List<PageLink> getPageLinks() {
        final List<PageLink> links = getPageLinks(docHandle, pageNo);
        if (links != null) {
            final float width = getWidth();
            final float height = getHeight();
            for (PageLink link : links) {
                link.sourceRect.left = link.sourceRect.left / width;
                link.sourceRect.right = link.sourceRect.right / width;
                link.sourceRect.top = link.sourceRect.top / height;
                link.sourceRect.bottom = link.sourceRect.bottom / height;

                if (link.url != null && link.url.startsWith("#")) {
                    try {
                        link.targetPage = Integer.parseInt(link.url.substring(1)) - 1;
                        link.url = null;
                    } catch (NumberFormatException ex) {
                        ex.printStackTrace();
                    }
                }
            }

            return links;
        }
        return Collections.emptyList();
    }

    private native static ArrayList<PageLink> getPageLinks(long docHandle, int pageNo);
}
