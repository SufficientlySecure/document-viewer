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
import android.graphics.Rect;
import android.graphics.RectF;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.artifex.mupdf.fitz.ColorSpace;
import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.DrawDevice;
import com.artifex.mupdf.fitz.Matrix;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.Pixmap;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;

import org.emdev.utils.LengthUtils;
import org.emdev.utils.MatrixUtils;

public class MuPdfPage extends AbstractCodecPage {

    private final Page pageHandle;
    private boolean recicled;

    final RectF pageBounds;
    final int actualWidth;
    final int actualHeight;

    private final static boolean USE_DIRECT = true;

    public MuPdfPage(final Page pageHandle) {
        this.pageHandle = pageHandle;
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
        final Matrix matrixArray = calculateFz(width, height, pageSliceBounds);
        return render(viewState, new Rect(0, 0, width, height), matrixArray);
    }

    private Matrix calculateFz(final int width, final int height, final RectF pageSliceBounds) {
        final Matrix matrix = new Matrix();
        // This should be corrected
        matrix.scale(width / pageBounds.width(), height / pageBounds.height());
        matrix.translate(-pageSliceBounds.left * width, -pageSliceBounds.top * height);
        matrix.scale(1 / pageSliceBounds.width(), 1 / pageSliceBounds.height());

        return matrix;
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

    public ByteBufferBitmap render(final ViewState viewState, final Rect viewbox, final Matrix ctm) {
        if (isRecycled()) {
            throw new RuntimeException("The page has been recycled before: " + this);
        }

        final Bitmap bm = AndroidDrawDevice.drawPage(pageHandle, ctm);
        final ByteBufferBitmap bmp = ByteBufferBitmap.get(bm);

        return bmp;
    }

    // @Override
    // public List<PageLink> getPageLinks() {
    //     return MuPdfLinks.getPageLinks(docHandle, pageHandle, pageBounds);
    // }

    // @Override
    // public List<? extends RectF> searchText(final String pattern) {
    //     final List<PageTextBox> rects = search(docHandle, pageHandle, pattern);
    //     if (LengthUtils.isNotEmpty(rects)) {
    //         final Set<String> temp = new HashSet<String>();
    //         final Iterator<PageTextBox> iter = rects.iterator();
    //         while (iter.hasNext()) {
    //             final PageTextBox b = iter.next();
    //             if (temp.add(b.toString())) {
    //                 b.left = (b.left - pageBounds.left) / pageBounds.width();
    //                 b.top = (b.top - pageBounds.top) / pageBounds.height();
    //                 b.right = (b.right - pageBounds.left) / pageBounds.width();
    //                 b.bottom = (b.bottom - pageBounds.top) / pageBounds.height();
    //             } else {
    //                 iter.remove();
    //             }
    //         }
    //     }
    //     return rects;
    // }
}
