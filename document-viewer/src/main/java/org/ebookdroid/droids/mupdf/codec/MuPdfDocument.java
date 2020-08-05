package org.ebookdroid.droids.mupdf.codec;

import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.Rect;

import android.graphics.RectF;

import java.util.List;
import java.util.ArrayList;

import java.io.File;

public class MuPdfDocument extends AbstractCodecDocument {

    protected final Document documentHandle;
    protected final String acceleratorPath;

    private List<OutlineLink> docOutline;

    MuPdfDocument(final MuPdfContext context, final String fname) {
        super(context);
        acceleratorPath = getAcceleratorPath(fname);
        if (acceleratorValid(fname, acceleratorPath))
			documentHandle = Document.openDocument(fname, acceleratorPath);
		else
			documentHandle = Document.openDocument(fname);

        documentHandle.saveAccelerator(acceleratorPath);
    }

    @Override
    public List<OutlineLink> getOutline() {
        if (docOutline == null) {
            docOutline = MuPdfOutline.getOutline(this);
            documentHandle.saveAccelerator(acceleratorPath);
        }
        return docOutline;
    }

    @Override
    public MuPdfPage getPage(final int pageNumber) {
        final MuPdfPage page = new MuPdfPage(this, documentHandle.loadPage(pageNumber));
        documentHandle.saveAccelerator(acceleratorPath);
        return page;
    }

    @Override
    public int getPageCount() {
        final int pages = documentHandle.countPages();
        documentHandle.saveAccelerator(acceleratorPath);
        return pages;
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
        documentHandle.saveAccelerator(acceleratorPath);
        return info;
    }

    @Override
    protected void freeDocument() {
        documentHandle.destroy();
    }

    @Override
    public Boolean needsPassword() {
        return documentHandle.needsPassword();
    }

    @Override
    public Boolean authenticate(final String password) {
        return documentHandle.authenticatePassword(password);
    }

    public Boolean isPDF() {
        return documentHandle.isPDF();
    }

    public void normalizeLinkTargetRect(final int page, final RectF rect) {

        final CodecPageInfo cpi = getPageInfo(page);

        final float left = rect.left;
        final float top = rect.top;

        if (((cpi.rotation / 90) % 2) != 0) {
            rect.right = rect.left = left / cpi.height;
            rect.bottom = rect.top = top / cpi.width;
        } else {
            rect.right = rect.left = left / cpi.width;
            rect.bottom = rect.top = top / cpi.height;
        }
    }

    protected static String getAcceleratorPath(String documentPath) {
		String acceleratorName = documentPath.substring(1);
		acceleratorName = acceleratorName.replace(File.separatorChar, '%');
		acceleratorName = acceleratorName.replace('\\', '%');
		acceleratorName = acceleratorName.replace(':', '%');
		String tmpdir = System.getProperty("java.io.tmpdir");
		return new StringBuffer(tmpdir).append(File.separatorChar).append(acceleratorName).append(".accel").toString();
	}

    protected static boolean acceleratorValid(String documentPath, String acceleratorPath) {
		long documentModified = new File(documentPath).lastModified();
		long acceleratorModified = new File(acceleratorPath).lastModified();
		return acceleratorModified != 0 && acceleratorModified > documentModified;
	}
}
