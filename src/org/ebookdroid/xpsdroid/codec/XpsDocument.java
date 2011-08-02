package org.ebookdroid.xpsdroid.codec;

import org.ebookdroid.core.OutlineLink;
import org.ebookdroid.core.PageLink;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;

import java.util.ArrayList;
import java.util.List;

public class XpsDocument implements CodecDocument {

    private long docHandle;
    private static final int FITZMEMORY = 512 * 1024;

    private XpsDocument(final long docHandle) {
        this.docHandle = docHandle;
    }

    @Override
    public List<OutlineLink> getOutline() {
        return null;
    }

    @Override
    public ArrayList<PageLink> getPageLinks(final int pageNumber) {
        return null;
    }

    @Override
    public CodecPage getPage(final int pageNumber) {
        return XpsPage.createPage(docHandle, pageNumber + 1);
    }

    @Override
    public int getPageCount() {
        return getPageCount(docHandle);
    }

    static XpsDocument openDocument(final String fname) {
        return new XpsDocument(open(FITZMEMORY, fname));
    }

    private static native long open(int fitzmemory, String fname);

    private static native void free(long handle);

    private static native int getPageCount(long handle);

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    @Override
    public synchronized void recycle() {
        if (docHandle != 0) {
            free(docHandle);
            docHandle = 0;
        }
    }

    @Override
    public CodecPageInfo getPageInfo(final int pageIndex, final CodecContext codecContext) {
        // TODO Fill info for PDF
        return null;
    }
}
