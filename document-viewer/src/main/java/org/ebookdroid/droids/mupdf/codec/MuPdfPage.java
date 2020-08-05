package org.ebookdroid.droids.mupdf.codec;

import org.ebookdroid.common.bitmaps.ByteBufferBitmap;
import org.ebookdroid.common.bitmaps.ByteBufferManager;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.core.codec.AbstractCodecPage;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.core.codec.PageTextBox;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.artifex.mupdf.fitz.ColorSpace;
import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Matrix;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.Quad;
import com.artifex.mupdf.fitz.Rect;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;

import org.emdev.utils.LengthUtils;

public class MuPdfPage extends AbstractCodecPage {

    private final MuPdfDocument documentHandle;
    private final Page pageHandle;
    private boolean recicled;

    final RectF pageBounds;
    final int actualWidth;
    final int actualHeight;

    public MuPdfPage(final MuPdfDocument documentHandle, final Page pageHandle) {
        this.pageHandle = pageHandle;
        this.documentHandle = documentHandle;
        this.pageBounds = getBounds();
        this.recicled = false;
        this.actualWidth = (int) pageBounds.width();
        this.actualHeight = (int) pageBounds.height();
    }

    @Override
    public int getWidth() {
        return actualWidth;
    }

    @Override
    public int getHeight() {
        return actualHeight;
    }

    @Override
    public ByteBufferBitmap renderBitmap(final ViewState viewState, final int width, final int height,
                                         final RectF pageSliceBounds) {
        if (isRecycled()) {
            throw new RuntimeException("The page has been recycled before: " + this);
        }

        // Original code (android.graphics.Matrix):
        //
        // final Matrix matrix = MatrixUtils.get();
        // matrix.postScale(width / pageBounds.width(), height / pageBounds.height());
        // a.translate(-pageSliceBounds.left * width, -pageSliceBounds.top * height);
        // matrix.postTranslate(-pageSliceBounds.left * width, -pageSliceBounds.top * height);
        //
        // MuPDF's version of Matrix does scale and translate differently

        final com.artifex.mupdf.fitz.Matrix ctm = new com.artifex.mupdf.fitz.Matrix();
        ctm.scale(width / pageBounds.width(), height / pageBounds.height());
        ctm.scale(1 / pageSliceBounds.width(), 1 / pageSliceBounds.height());
        ctm.e = -pageSliceBounds.left * width / pageSliceBounds.width();
        ctm.f = -pageSliceBounds.top * height / pageSliceBounds.height();

		Bitmap bm = Bitmap.createBitmap(width, height,
                                        Bitmap.Config.ARGB_8888);
        AndroidDrawDevice dev = new AndroidDrawDevice(bm, 0, 0, 0, 0, width, height);
		pageHandle.run(dev, ctm, null);
		dev.close();
		dev.destroy();

        final ByteBufferBitmap bmp = ByteBufferBitmap.get(bm);

        return bmp;
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    @Override
    public synchronized void recycle() {
        if (!recicled) {
            recicled = true;
            pageHandle.destroy();
        }
    }

    @Override
    public boolean isRecycled() {
        return recicled;
    }

    private RectF getBounds() {
        final com.artifex.mupdf.fitz.Rect box = pageHandle.getBounds();
        return new RectF(box.x0, box.y0, box.x1, box.y1);
    }

    @Override
    public List<PageLink> getPageLinks() {
        return MuPdfLinks.getPageLinks(documentHandle, pageHandle, pageBounds);
    }

    @Override
    public List<? extends RectF> searchText(final String pattern) {
        final Quad[] hits = pageHandle.search(pattern);
        final ArrayList<RectF> rects = new ArrayList<RectF>(hits != null ? hits.length : 0);
        if (hits != null) {
            for (Quad hit : hits) {
                final Rect rect = hit.toRect();
                rects.add(new RectF((rect.x0 - pageBounds.left) / pageBounds.width(),
                                    (rect.y0 - pageBounds.top) / pageBounds.height(),
                                    (rect.x1 - pageBounds.left) / pageBounds.width(),
                                    (rect.y1 - pageBounds.top) / pageBounds.height()));
            }
        }
        return rects;
    }
}
