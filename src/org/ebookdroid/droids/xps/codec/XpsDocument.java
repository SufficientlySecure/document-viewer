package org.ebookdroid.droids.xps.codec;

import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;

import android.graphics.RectF;

import java.util.List;

public class XpsDocument extends AbstractCodecDocument {

    //TODO: Must be configurable
    private static final int STOREMEMORY = 64<<20;

    XpsDocument(final XpsContext context, final String fname) {
        super(context, open(STOREMEMORY, fname));
    }
    
    @Override
    public List<OutlineLink> getOutline() {
        final XpsOutline ou = new XpsOutline();
        return ou.getOutline(documentHandle);
    }

    @Override
    public CodecPage getPage(final int pageNumber) {
        return XpsPage.createPage(documentHandle, pageNumber + 1);
    }

    @Override
    public int getPageCount() {
        return getPageCount(documentHandle);
    }

    @Override
    public CodecPageInfo getPageInfo(final int pageNumber) {
        final CodecPageInfo info = new CodecPageInfo();
        final int res = getPageInfo(documentHandle, pageNumber + 1, info);
        if (res == -1) {
            return null;
        } else {
            info.width = (XpsContext.getWidthInPixels(info.width));
            info.height = (XpsContext.getHeightInPixels(info.height));
            return info;
        }
    }

    @Override
    protected void freeDocument() {
        free(documentHandle);
    }

    private native static int getPageInfo(long docHandle, int pageNumber, CodecPageInfo cpi);

    private static native long open(int storememory, String fname);

    private static native void free(long handle);

    private static native int getPageCount(long handle);

    @Override
    public List<? extends RectF> searchText(final int pageNuber, final String pattern) throws DocSearchNotSupported {
        return null;
    }
}
