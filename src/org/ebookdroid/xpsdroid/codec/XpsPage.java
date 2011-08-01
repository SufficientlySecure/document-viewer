package org.ebookdroid.xpsdroid.codec;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;

import org.ebookdroid.core.codec.CodecPage;

public class XpsPage implements CodecPage
{
    private long pageHandle;
    private long docHandle;

    private XpsPage(long pageHandle, long docHandle)
    {
        this.pageHandle = pageHandle;
        this.docHandle = docHandle;
    }

    public int getWidth()
    {
        return (int) getPageWidth(pageHandle);
    }

    public int getHeight()
    {
        return (int) getPageHeight(pageHandle);
    }

    public Bitmap renderBitmap(int width, int height, RectF pageSliceBounds)
    {
        Matrix matrix = new Matrix();
        matrix.postScale(width / (float)getPageWidth(pageHandle), height / (float)getPageHeight(pageHandle));
        matrix.postTranslate(-pageSliceBounds.left*width, -pageSliceBounds.top*height);
        matrix.postScale(1/pageSliceBounds.width(), 1/pageSliceBounds.height());
        return render(new Rect(0,0,width,height), matrix);
    }

    static XpsPage createPage(long dochandle, int pageno)
    {
        return new XpsPage(open(dochandle, pageno), dochandle);
    }

    @Override
    protected void finalize() throws Throwable
    {
        recycle();
        super.finalize();
    }
    
    public synchronized void recycle() {
    	if (pageHandle != 0) {
    		free(pageHandle);
    		pageHandle = 0;
    	}
	}


    public Bitmap render(Rect viewbox, Matrix matrix)
	{
        int[] mRect = new int[4];
        mRect[0] = viewbox.left;
		mRect[1] = viewbox.top;
		mRect[2] = viewbox.right;
		mRect[3] = viewbox.bottom;

        float[] matrixSource = new float[9];
        float[] matrixArray = new float[6];
        matrix.getValues(matrixSource);
		matrixArray[0] = matrixSource[0];
		matrixArray[1] = matrixSource[3];
		matrixArray[2] = matrixSource[1];
		matrixArray[3] = matrixSource[4];
		matrixArray[4] = matrixSource[2];
		matrixArray[5] = matrixSource[5];

        int width = viewbox.width();
        int height = viewbox.height();
        int[] bufferarray = new int[width * height];
        renderPage(docHandle, pageHandle, mRect, matrixArray, bufferarray);
        return Bitmap.createBitmap(bufferarray, width, height, Bitmap.Config.RGB_565);
	}
    
    private static native int getPageWidth(long handle);
    
    private static native int getPageHeight(long handle);

    private static native void free(long handle);

    private static native long open(long dochandle, int pageno);

    private static native void renderPage(long dochandle, long pagehandle,
    		int[] viewboxarray, float[] matrixarray,
    		int[] bufferarray);
}
