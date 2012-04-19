package org.ebookdroid.droids.xps.codec;

import org.ebookdroid.EBookDroidLibraryLoader;
import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.core.codec.PageTextBox;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.Collections;
import java.util.List;

import org.emdev.utils.MatrixUtils;

public class XpsPage implements CodecPage {

    private long pageHandle;
    private final long docHandle;
    private final RectF pageBounds;

    private XpsPage(final long pageHandle, final long docHandle) {
        this.pageHandle = pageHandle;
        this.docHandle = docHandle;
        this.pageBounds = getBounds();
    }

    @Override
    public List<PageLink> getPageLinks() {
        return Collections.emptyList();
    }

    @Override
    public int getWidth() {
        return XpsContext.getWidthInPixels(pageBounds.width());
    }

    @Override
    public int getHeight() {
        return XpsContext.getHeightInPixels(pageBounds.height());
    }

    private RectF getBounds() {
        final float[] box = new float[4];
        getBounds(docHandle, pageHandle, box);
        return new RectF(box[0], box[1], box[2], box[3]);
    }

    @Override
    public BitmapRef renderBitmap(final int width, final int height, final RectF pageSliceBounds) {
        final Matrix matrix = MatrixUtils.get();
        matrix.postScale(width / (float) pageBounds.width(), height / (float) pageBounds.height());
        matrix.postTranslate(-pageSliceBounds.left * width, -pageSliceBounds.top * height);
        matrix.postScale(1 / pageSliceBounds.width(), 1 / pageSliceBounds.height());
        return render(new Rect(0, 0, width, height), matrix);
    }

    static XpsPage createPage(final long dochandle, final int pageno) {
        return new XpsPage(open(dochandle, pageno), dochandle);
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    @Override
    public synchronized void recycle() {
        if (pageHandle != 0) {
            free(docHandle, pageHandle);
            pageHandle = 0;
        }
    }

    @Override
    public synchronized boolean isRecycled() {
        return pageHandle == 0;
    }

    public BitmapRef render(final Rect viewbox, final Matrix matrix) {
        final int[] mRect = new int[4];
        mRect[0] = viewbox.left;
        mRect[1] = viewbox.top;
        mRect[2] = viewbox.right;
        mRect[3] = viewbox.bottom;

        final float[] matrixSource = new float[9];
        final float[] matrixArray = new float[6];
        matrix.getValues(matrixSource);
        matrixArray[0] = matrixSource[0];
        matrixArray[1] = matrixSource[3];
        matrixArray[2] = matrixSource[1];
        matrixArray[3] = matrixSource[4];
        matrixArray[4] = matrixSource[2];
        matrixArray[5] = matrixSource[5];

        final int width = viewbox.width();
        final int height = viewbox.height();

        if (EBookDroidLibraryLoader.nativeGraphicsAvailable && SettingsManager.getAppSettings().useNativeGraphics) {
            final BitmapRef bmp = BitmapManager.getBitmap("PDF page", width, height, XpsContext.NATIVE_BITMAP_CFG);
            if (renderPageBitmap(docHandle, pageHandle, mRect, matrixArray, bmp.getBitmap())) {
                return bmp;
            } else {
                BitmapManager.release(bmp);
                return null;
            }
        }

        final int[] bufferarray = new int[width * height];
        renderPage(docHandle, pageHandle, mRect, matrixArray, bufferarray);
        BitmapRef b = BitmapManager.getBitmap("XPS page", width, height, XpsContext.BITMAP_CFG);
        b.getBitmap().setPixels(bufferarray, 0, width, 0, 0, width, height);
        return b;
    }

    @Override
    public List<PageTextBox> getPageText() {
        return Collections.emptyList();
    }

    @Override
    public List<? extends RectF> searchText(String pattern) {
        return Collections.emptyList();
    }

    private static native void getBounds(long dochandle, long handle, float[] bounds);

    private static native void free(long dochandle, long handle);

    private static native long open(long dochandle, int pageno);

    private static native void renderPage(long dochandle, long pagehandle, int[] viewboxarray, float[] matrixarray,
            int[] bufferarray);

    private static native boolean renderPageBitmap(long dochandle, long pagehandle, int[] viewboxarray,
            float[] matrixarray, Bitmap bitmap);
}
