package org.ebookdroid.xpsdroid.codec;

import java.util.ArrayList;
import java.util.List;

import org.ebookdroid.core.OutlineLink;
import org.ebookdroid.core.PageLink;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;

public class XpsDocument implements CodecDocument
{
    private long docHandle;
    private static final int FITZMEMORY = 512 * 1024;

    private XpsDocument(long docHandle)
    {
        this.docHandle = docHandle;        
    }
    
    public List<OutlineLink> getOutline()
    {
        return null;    
    }
    
    public ArrayList<PageLink> getPageLinks(int pageNumber)
    {
    	return null;
    }

    public CodecPage getPage(int pageNumber)
    {
        return XpsPage.createPage(docHandle, pageNumber + 1);
    }

    public int getPageCount()
    {
        return getPageCount(docHandle);
    }
    
    static XpsDocument openDocument(String fname)
    {
        return new XpsDocument(open(FITZMEMORY, fname));
    }
    
    private static native long open(int fitzmemory, String fname);

    private static native void free(long handle);

    private static native int getPageCount(long handle);

    @Override
    protected void finalize() throws Throwable
    {
        recycle();
        super.finalize();
    }
    public synchronized void recycle() {
    	if (docHandle != 0) {
    		free(docHandle);
    		docHandle = 0;
    	}
    }

	@Override
	public CodecPageInfo getPageInfo(int pageIndex, CodecContext codecContext) {
		// TODO Fill info for PDF
		return null;
	}
}
