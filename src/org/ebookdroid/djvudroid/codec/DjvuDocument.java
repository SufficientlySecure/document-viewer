package org.ebookdroid.djvudroid.codec;

import java.util.ArrayList;
import java.util.List;

import org.ebookdroid.core.OutlineLink;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.PageLink;

public class DjvuDocument implements CodecDocument
{
    private long documentHandle;

    private DjvuDocument(long documentHandle)
    {
        this.documentHandle = documentHandle;
    }

    static DjvuDocument openDocument(String fileName, DjvuContext djvuContext)
    {
        return new DjvuDocument(open(djvuContext.getContextHandle(), fileName));
    }
    
    public List<OutlineLink> getOutline()
    {
    	 DjvuOutline ou = new DjvuOutline();
         return ou.getOutline(documentHandle);    
    }

    private native static long open(long contextHandle, String fileName);
    private native static long getPage(long docHandle, int pageNumber);
    private native static int getPageCount(long docHandle);
    private native static void free(long pageHandle);
    
    private native static ArrayList<PageLink> getPageLinks(long docHandle, int pageNumber);

    public ArrayList<PageLink> getPageLinks(int pageNumber)
    {
    	return getPageLinks(documentHandle, pageNumber);
    }
    
    public DjvuPage getPage(int pageNumber)
    {
        return new DjvuPage(getPage(documentHandle, pageNumber));
    }

    public int getPageCount()
    {
        return getPageCount(documentHandle);
    }

    @Override
    protected void finalize() throws Throwable
    {
        recycle();
        super.finalize();
    }
    
    public synchronized void recycle() {
    	if (documentHandle == 0) {
    		return;
    	}
    	free(documentHandle);
    	documentHandle = 0;
	}

    private native static int getPageInfo(long docHandle, int pageNumber, long contextHandle, CodecPageInfo cpi);
    
    public CodecPageInfo getPageInfo(int pageNumber, CodecContext ctx) {
    	CodecPageInfo info = new CodecPageInfo();
    	int res = getPageInfo(documentHandle, pageNumber, ctx.getContextHandle(), info);
    	if (res == -1) {
    		return null;
    	} else {
    		return info;
    	}
    }
}

