package org.ebookdroid.core.codec;

import java.util.ArrayList;
import java.util.List;

import org.ebookdroid.core.OutlineLink;
import org.ebookdroid.core.PageLink;

public interface CodecDocument
{
    CodecPage getPage(int pageNumber);

    int getPageCount();
    
    public List<OutlineLink> getOutline();
    
    public ArrayList<PageLink> getPageLinks(int pageNuber);
    
    void recycle();

	CodecPageInfo getPageInfo(int pageIndex, CodecContext codecContext);
}
