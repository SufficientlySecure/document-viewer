package org.ebookdroid.core.codec;

import android.graphics.Bitmap;
import android.graphics.RectF;

import java.util.List;

public interface CodecDocument {

    int getPageCount();

    CodecPage getPage(int pageNuber);

    CodecPageInfo getUnifiedPageInfo();

    CodecPageInfo getPageInfo(int pageNuber);

    List<? extends RectF> searchText(int pageNuber, final String pattern) throws DocSearchNotSupported;

    List<OutlineLink> getOutline();

    void recycle();

    /**
     * @return <code>true</code> if instance has been recycled
     */
    boolean isRecycled();

    Bitmap getEmbeddedThumbnail();

    public static class DocSearchNotSupported extends Exception {

        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 6741243859033574916L;

    }
}
