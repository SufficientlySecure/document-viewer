package org.ebookdroid.core.codec;


import android.graphics.Bitmap;

import java.util.List;

public interface CodecDocument {

    int getPageCount();

    CodecPage getPage(int pageNuber);

    CodecPageInfo getUnifiedPageInfo();

    CodecPageInfo getPageInfo(int pageNuber);

    List<OutlineLink> getOutline();

    void recycle();

    /**
     * @return <code>true</code> if instance has been recycled
     */
    boolean isRecycled();

    Bitmap getEmbeddedThumbnail();
}
