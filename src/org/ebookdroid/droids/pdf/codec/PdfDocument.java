package org.ebookdroid.droids.pdf.codec;

import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;

import android.graphics.RectF;

import java.util.List;

public class PdfDocument extends AbstractCodecDocument {

    // TODO: Must be configurable
    private static final int STOREMEMORY = 64 << 20;

    PdfDocument(final PdfContext context, final String fname, final String pwd) {
        super(context, open(STOREMEMORY, fname, pwd));
    }

    @Override
    public List<OutlineLink> getOutline() {
        final PdfOutline ou = new PdfOutline();
        return ou.getOutline(documentHandle);
    }

    @Override
    public CodecPage getPage(final int pageNumber) {
        return PdfPage.createPage(documentHandle, pageNumber + 1);
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
            info.width = (PdfContext.getWidthInPixels(info.width));
            info.height = (PdfContext.getHeightInPixels(info.height));
            return info;
        }
    }

    @Override
    protected void freeDocument() {
        free(documentHandle);
    }

    static void normalizeLinkTargetRect(final long docHandle, final int targetPage, final RectF targetRect) {
        final CodecPageInfo cpi = new CodecPageInfo();
        PdfDocument.getPageInfo(docHandle, targetPage, cpi);

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

    private static native long open(int storememory, String fname, String pwd);

    private static native void free(long handle);

    private static native int getPageCount(long handle);

    @Override
    public List<? extends RectF> searchText(final int pageNuber, final String pattern) throws DocSearchNotSupported {
        return null;
    }
}
