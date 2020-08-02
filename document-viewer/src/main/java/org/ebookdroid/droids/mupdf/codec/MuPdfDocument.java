package org.ebookdroid.droids.mupdf.codec;

import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;

import org.ebookdroid.droids.mupdf.codec.MuPdfPage;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Outline;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.Rect;

import android.graphics.RectF;

import java.util.List;

public class MuPdfDocument extends AbstractCodecDocument {

    protected final Document documentHandle;

    MuPdfDocument(final MuPdfContext context, final String fname, final String pwd) {
        super(context);
        documentHandle = Document.openDocument(fname);
        if (documentHandle.needsPassword()) {
            documentHandle.authenticatePassword(pwd);
        }
    }

    // @Override
    // public List<OutlineLink> getOutline() {
    //     final MuPdfOutline ou = new MuPdfOutline();
    //     return ou.getOutline(documentHandle);
    // }

    @Override
    public MuPdfPage getPage(final int pageNumber) {
         return new MuPdfPage(documentHandle.loadPage(pageNumber));
    }

    @Override
    public int getPageCount() {
         return documentHandle.countPages();
    }

    @Override
    public CodecPageInfo getPageInfo(final int pageNumber) {
        final CodecPageInfo info = new CodecPageInfo();
        final MuPdfPage page = getPage(pageNumber);
        info.width = page.getWidth();
        info.height = page.getHeight();
        info.dpi = 0;
        info.rotation = 0;
        info.version = 0;
        return info;
    }

    @Override
    protected void freeDocument() {
        documentHandle.destroy();
    }

    static void normalizeLinkTargetRect(final long docHandle, final int targetPage, final RectF targetRect,
            final int flags) {

        // final CodecPageInfo cpi = new CodecPageInfo();
        // MuPdfDocument.getPageInfo(docHandle, targetPage, cpi);

        // final float left = targetRect.left;
        // final float top = targetRect.top;

        // if (((cpi.rotation / 90) % 2) != 0) {
        //     targetRect.right = targetRect.left = left / cpi.height;
        //     targetRect.bottom = targetRect.top = top / cpi.width;
        // } else {
        //     targetRect.right = targetRect.left = left / cpi.width;
        //     targetRect.bottom = targetRect.top = top / cpi.height;
        // }
    }
}
