package org.ebookdroid.droids.djvu.codec;

import org.ebookdroid.common.bitmaps.ByteBufferBitmap;
import org.ebookdroid.common.bitmaps.ByteBufferManager;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.core.codec.AbstractCodecPage;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.core.codec.PageTextBox;

import android.graphics.Color;
import android.graphics.RectF;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.emdev.utils.LengthUtils;

public class DjvuPage extends AbstractCodecPage {

    private final long contextHandle;
    private final long docHandle;
    private final int pageNo;
    private long pageHandle;

    private final static boolean USE_DIRECT = true;

    DjvuPage(final long contextHandle, final long docHandle, final long pageHandle, final int pageNo) {
        this.contextHandle = contextHandle;
        this.docHandle = docHandle;
        this.pageHandle = pageHandle;
        this.pageNo = pageNo;
    }

    @Override
    public int getWidth() {
        return getWidth(pageHandle);
    }

    @Override
    public int getHeight() {
        return getHeight(pageHandle);
    }

    @Override
    public ByteBufferBitmap renderBitmap(final ViewState viewState, final int width, final int height,
            final RectF pageSliceBounds) {
        final int renderMode = AppSettings.current().djvuRenderingMode;

        ByteBufferBitmap buf = ByteBufferManager.getBitmap(width, height);
        if (width > 0 && height > 0) {

            final ByteBuffer byteBuffer = buf.getPixels();

            if (renderPageDirect(pageHandle, contextHandle, width, height, pageSliceBounds.left, pageSliceBounds.top,
                    pageSliceBounds.width(), pageSliceBounds.height(), byteBuffer, renderMode)) {
                return buf;
            }
        }

        buf.eraseColor(Color.GRAY);
        return buf;
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
            for (final PageLink link : links) {
                normalize(link.sourceRect, width, height);

                if (link.url != null && link.url.startsWith("#")) {
                    try {
                        link.targetPage = Integer.parseInt(link.url.substring(1)) - 1;
                        link.url = null;
                    } catch (final NumberFormatException ex) {
                        ex.printStackTrace();
                    }
                }
            }

            return links;
        }
        return Collections.emptyList();
    }

    @Override
    public List<PageTextBox> getPageText() {
        final List<PageTextBox> list = getPageText(docHandle, pageNo, contextHandle, null);
        if (LengthUtils.isNotEmpty(list)) {
            final float width = getWidth();
            final float height = getHeight();
            for (final PageTextBox ptb : list) {
                normalizeTextBox(ptb, width, height);
                // System.out.println("" + ptb);
            }
        }
        return list;
    }

    @Override
    public List<? extends RectF> searchText(final String pattern) {
        final List<PageTextBox> list = getPageText(docHandle, pageNo, contextHandle, pattern);
        if (LengthUtils.isNotEmpty(list)) {
            final float width = getWidth();
            final float height = getHeight();
            for (final PageTextBox ptb : list) {
                normalizeTextBox(ptb, width, height);
            }
        }
        return list;
    }

    static void normalize(final RectF r, final float width, final float height) {
        r.left = r.left / width;
        r.right = r.right / width;
        r.top = r.top / height;
        r.bottom = r.bottom / height;
    }

    static void normalizeTextBox(final PageTextBox r, final float width, final float height) {
        final float left = r.left / width;
        final float right = r.right / width;
        final float top = 1 - r.top / height;
        final float bottom = 1 - r.bottom / height;
        r.left = Math.min(left, right);
        r.right = Math.max(left, right);
        r.top = Math.min(top, bottom);
        r.bottom = Math.max(top, bottom);
    }

    private static native int getWidth(long pageHandle);

    private static native int getHeight(long pageHandle);

    private static native boolean isDecodingDone(long pageHandle);

    private static native boolean renderPageDirect(long pageHandle, long contextHandle, int targetWidth,
            int targetHeight, float pageSliceX, float pageSliceY, float pageSliceWidth, float pageSliceHeight,
            ByteBuffer byteBuffer, int renderMode);

    private static native void free(long pageHandle);

    private native static ArrayList<PageLink> getPageLinks(long docHandle, int pageNo);

    native static List<PageTextBox> getPageText(long docHandle, int pageNo, long contextHandle, String pattern);

}
