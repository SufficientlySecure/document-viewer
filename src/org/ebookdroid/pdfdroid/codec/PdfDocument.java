package org.ebookdroid.pdfdroid.codec;

import java.util.ArrayList;
import java.util.List;

import org.ebookdroid.core.OutlineLink;
import org.ebookdroid.core.PageLink;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;

public class PdfDocument implements CodecDocument
{
    private long docHandle;
    private static final int FITZMEMORY = 512 * 1024;

    private PdfDocument(long docHandle)
    {
        this.docHandle = docHandle;        
    }
    
    public List<OutlineLink> getOutline()
    {
    	PdfOutline ou = new PdfOutline();
        return ou.getOutline(docHandle);    
    }
    
    public ArrayList<PageLink> getPageLinks(int pageNumber)
    {
    	return getPageLinks(docHandle, pageNumber);
    }

    public CodecPage getPage(int pageNumber)
    {
        return PdfPage.createPage(docHandle, pageNumber + 1);
    }

    public int getPageCount()
    {
        return getPageCount(docHandle);
    }
    
    static PdfDocument openDocument(String fname, String pwd)
    {
        return new PdfDocument(open(FITZMEMORY, fname, pwd));
    }
    
    private native static ArrayList<PageLink> getPageLinks(long docHandle, int pageNumber);

    private static native long open(int fitzmemory, String fname, String pwd);

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
