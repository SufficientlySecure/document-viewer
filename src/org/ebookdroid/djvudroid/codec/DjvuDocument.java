package org.ebookdroid.djvudroid.codec;

import org.ebookdroid.core.OutlineLink;
import org.ebookdroid.core.PageLink;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPageInfo;

import java.util.ArrayList;
import java.util.List;

public class DjvuDocument implements CodecDocument {

    private long documentHandle;

    private DjvuDocument(final long documentHandle) {
        this.documentHandle = documentHandle;
    }

    static DjvuDocument openDocument(final String fileName, final DjvuContext djvuContext) {
        return new DjvuDocument(open(djvuContext.getContextHandle(), fileName));
    }

    @Override
    public List<OutlineLink> getOutline() {
        final DjvuOutline ou = new DjvuOutline();
        return ou.getOutline(documentHandle);
    }

    private native static long open(long contextHandle, String fileName);

    private native static long getPage(long docHandle, int pageNumber);

    private native static int getPageCount(long docHandle);

    private native static void free(long pageHandle);

    private native static ArrayList<PageLink> getPageLinks(long docHandle, int pageNumber);

    @Override
    public ArrayList<PageLink> getPageLinks(final int pageNumber) {
        return getPageLinks(documentHandle, pageNumber);
    }

    @Override
    public DjvuPage getPage(final int pageNumber) {
        return new DjvuPage(getPage(documentHandle, pageNumber));
    }

    @Override
    public int getPageCount() {
        return getPageCount(documentHandle);
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    @Override
    public synchronized void recycle() {
        if (documentHandle == 0) {
            return;
        }
        free(documentHandle);
        documentHandle = 0;
    }

    private native static int getPageInfo(long docHandle, int pageNumber, long contextHandle, CodecPageInfo cpi);

    @Override
    public CodecPageInfo getPageInfo(final int pageNumber, final CodecContext ctx) {
        final CodecPageInfo info = new CodecPageInfo();
        final int res = getPageInfo(documentHandle, pageNumber, ctx.getContextHandle(), info);
        if (res == -1) {
            return null;
        } else {
            return info;
        }
    }
}
