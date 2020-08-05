package org.ebookdroid.core.codec;

import android.graphics.Bitmap;

public interface CodecContext extends CodecFeatures {

    /**
     * Open appropriate document
     *
     * @param fileName
     *            document file name
     * @return an instance of a document
     */
    CodecDocument openDocument(String fileName);

    /**
     * @return context handler
     */
    long getContextHandle();

    /**
     * Recycle instance.
     */
    void recycle();

    /**
     * @return <code>true</code> if instance has been recycled
     */
    boolean isRecycled();

    Bitmap.Config getBitmapConfig();
}
