package org.ebookdroid.core.codec;

import org.ebookdroid.core.OutlineLink;
import org.ebookdroid.core.PageLink;

import java.util.List;

public interface CodecDocument {

    int getPageCount();

    CodecPage getPage(int pageNuber);

    CodecPageInfo getPageInfo(int pageNuber);

    List<PageLink> getPageLinks(int pageNuber);

    List<OutlineLink> getOutline();

    void recycle();

    /**
     * @return <code>true</code> if instance has been recycled
     */
    boolean isRecycled();
}
