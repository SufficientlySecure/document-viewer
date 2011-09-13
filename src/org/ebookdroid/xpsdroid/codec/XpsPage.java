package org.ebookdroid.xpsdroid.codec;

import org.ebookdroid.core.bitmaps.BitmapManager;
import org.ebookdroid.core.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.CodecPage;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;

public class XpsPage implements CodecPage {

    private long pageHandle;
    private final long docHandle;

    private XpsPage(final long pageHandle, final long docHandle) {
        this.pageHandle = pageHandle;
        this.docHandle = docHandle;
    }

    @Override
    public int getWidth() {
        return getPageWidth(pageHandle);
    }

    @Override
    public int getHeight() {
        return getPageHeight(pageHandle);
    }

    @Override
    public BitmapRef renderBitmap(final int width, final int height, final RectF pageSliceBounds) {
        final Matrix matrix = new Matrix();
        matrix.postScale(width / (float) getPageWidth(pageHandle), height / (float) getPageHeight(pageHandle));
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
        final int[] bufferarray = new int[width * height];
        renderPage(docHandle, pageHandle, mRect, matrixArray, bufferarray);
        BitmapRef b = BitmapManager.getBitmap(width, height, Bitmap.Config.RGB_565);
        b.getBitmap().setPixels(bufferarray, 0, width, 0, 0, width, height);
        return b;
    }

    private static native int getPageWidth(long handle);

    private static native int getPageHeight(long handle);

    private static native void free(long dochandle, long handle);

    private static native long open(long dochandle, int pageno);

    private static native void renderPage(long dochandle, long pagehandle, int[] viewboxarray, float[] matrixarray,
            int[] bufferarray);
}
