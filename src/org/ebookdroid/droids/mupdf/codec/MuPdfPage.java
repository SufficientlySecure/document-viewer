package org.ebookdroid.droids.mupdf.codec;

import org.ebookdroid.EBookDroidLibraryLoader;
import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.core.codec.PageTextBox;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.emdev.utils.LengthUtils;
import org.emdev.utils.MatrixUtils;

public class MuPdfPage implements CodecPage {

    private long pageHandle;
    private final long docHandle;

    final RectF pageBounds;
    final int actualWidth;
    final int actualHeight;

    private MuPdfPage(final long pageHandle, final long docHandle) {
        this.pageHandle = pageHandle;
        this.docHandle = docHandle;
        this.pageBounds = getBounds();
        // this.actualWidth = AbstractCodecContext.getWidthInPixels(pageBounds.width());
        // this.actualHeight = AbstractCodecContext.getHeightInPixels(pageBounds.height());
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
    public BitmapRef renderBitmap(final int width, final int height, final RectF pageSliceBounds) {
        final float[] matrixArray = calculateFz(width, height, pageSliceBounds);
        return render(new Rect(0, 0, width, height), matrixArray);
    }

    private float[] calculateFz(final int width, final int height, final RectF pageSliceBounds) {
        final Matrix matrix = MatrixUtils.get();
        matrix.postScale(width / pageBounds.width(), height / pageBounds.height());
        matrix.postTranslate(-pageSliceBounds.left * width, -pageSliceBounds.top * height);
        matrix.postScale(1 / pageSliceBounds.width(), 1 / pageSliceBounds.height());

        final float[] matrixSource = new float[9];
        matrix.getValues(matrixSource);

        final float[] matrixArray = new float[6];

        matrixArray[0] = matrixSource[0];
        matrixArray[1] = matrixSource[3];
        matrixArray[2] = matrixSource[1];
        matrixArray[3] = matrixSource[4];
        matrixArray[4] = matrixSource[2];
        matrixArray[5] = matrixSource[5];

        return matrixArray;
    }

    static MuPdfPage createPage(final long dochandle, final int pageno) {
        return new MuPdfPage(open(dochandle, pageno), dochandle);
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
    public boolean isRecycled() {
        return pageHandle == 0;
    }

    private RectF getBounds() {
        final float[] box = new float[4];
        getBounds(docHandle, pageHandle, box);
        return new RectF(box[0], box[1], box[2], box[3]);
    }

    public BitmapRef render(final Rect viewbox, final float[] ctm) {
        final int[] mRect = new int[4];
        mRect[0] = viewbox.left;
        mRect[1] = viewbox.top;
        mRect[2] = viewbox.right;
        mRect[3] = viewbox.bottom;

        final int width = viewbox.width();
        final int height = viewbox.height();

        if (EBookDroidLibraryLoader.nativeGraphicsAvailable && AppSettings.current().useNativeGraphics) {
            final BitmapRef bmp = BitmapManager.getBitmap("PDF page", width, height, MuPdfContext.NATIVE_BITMAP_CFG);
            if (renderPageBitmap(docHandle, pageHandle, mRect, ctm, bmp.getBitmap())) {
                return bmp;
            } else {
                BitmapManager.release(bmp);
                return null;
            }
        }

        final int[] bufferarray = new int[width * height];
        renderPage(docHandle, pageHandle, mRect, ctm, bufferarray);
        final BitmapRef b = BitmapManager.getBitmap("PDF page", width, height, MuPdfContext.BITMAP_CFG);
        b.getBitmap().setPixels(bufferarray, 0, width, 0, 0, width, height);
        return b;
    }

    @Override
    public List<PageLink> getPageLinks() {
        return MuPdfLinks.getPageLinks(docHandle, pageHandle, pageBounds);
    }

    private static native void getBounds(long dochandle, long handle, float[] bounds);

    private static native void free(long dochandle, long handle);

    private static native long open(long dochandle, int pageno);

    private static native void renderPage(long dochandle, long pagehandle, int[] viewboxarray, float[] matrixarray,
            int[] bufferarray);

    private static native boolean renderPageBitmap(long dochandle, long pagehandle, int[] viewboxarray,
            float[] matrixarray, Bitmap bitmap);

    private native static List<PageTextBox> search(long docHandle, long pageHandle, String pattern);

    @Override
    public List<PageTextBox> getPageText() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends RectF> searchText(final String pattern) {
        final List<PageTextBox> rects = search(docHandle, pageHandle, pattern);
        if (LengthUtils.isNotEmpty(rects)) {
            final Set<String> temp = new HashSet<String>();
            final Iterator<PageTextBox> iter = rects.iterator();
            while (iter.hasNext()) {
                final PageTextBox b = iter.next();
                if (temp.add(b.toString())) {
                    b.left = (b.left - pageBounds.left) / pageBounds.width();
                    b.top = (b.top - pageBounds.top) / pageBounds.height();
                    b.right = (b.right - pageBounds.left) / pageBounds.width();
                    b.bottom = (b.bottom - pageBounds.top) / pageBounds.height();
                } else {
                    iter.remove();
                }
            }
        }
        return rects;
    }
}
