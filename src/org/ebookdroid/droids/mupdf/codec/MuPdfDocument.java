package org.ebookdroid.droids.mupdf.codec;

import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;

import android.graphics.RectF;

import java.util.List;

public class MuPdfDocument extends AbstractCodecDocument {

    public static final int FORMAT_PDF = 0;
    public static final int FORMAT_XPS = 1;
    
    // TODO: Must be configurable
    private static final int STOREMEMORY = 64 << 20;

    MuPdfDocument(final MuPdfContext context, int format, final String fname, final String pwd) {
        super(context, open(STOREMEMORY, format, fname, pwd));
    }

    @Override
    public List<OutlineLink> getOutline() {
        final MuPdfOutline ou = new MuPdfOutline();
        return ou.getOutline(documentHandle);
    }

    @Override
    public CodecPage getPage(final int pageNumber) {
        return MuPdfPage.createPage(documentHandle, pageNumber + 1);
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
            // Check rotation
            info.rotation = (360 + info.rotation) % 360;
            //info.width = (MuPdfContext.getWidthInPixels(info.width));
            //info.height = (MuPdfContext.getHeightInPixels(info.height));
            return info;
        }
    }

    @Override
    protected void freeDocument() {
        free(documentHandle);
    }

    static void normalizeLinkTargetRect(final long docHandle, final int targetPage, final RectF targetRect) {
        final CodecPageInfo cpi = new CodecPageInfo();
        MuPdfDocument.getPageInfo(docHandle, targetPage, cpi);

        final float left = targetRect.left;
        final float top = targetRect.top;

        if (((cpi.rotation / 90) % 2) != 0) {
            targetRect.right = targetRect.left = left / cpi.height;
            targetRect.bottom = targetRect.top = 1.0f - top / cpi.width;
        } else {
            targetRect.right = targetRect.left = left / cpi.width;
            targetRect.bottom = targetRect.top = 1.0f - top / cpi.height;
        }
    }

    native static int getPageInfo(long docHandle, int pageNumber, CodecPageInfo cpi);

    private static native long open(int storememory, int format, String fname, String pwd);

    private static native void free(long handle);

    private static native int getPageCount(long handle);

    @Override
    public List<? extends RectF> searchText(int pageNuber, String pattern) throws DocSearchNotSupported {
        throw new DocSearchNotSupported();
    }

}
