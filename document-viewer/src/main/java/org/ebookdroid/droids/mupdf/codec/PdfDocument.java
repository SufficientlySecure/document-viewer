package org.ebookdroid.droids.mupdf.codec;

import com.artifex.mupdf.fitz.PDFDocument;

public class PdfDocument extends MuPdfDocument {

    PdfDocument(final MuPdfContext context, final String fname) {
       super(context, fname);
    }

    @Override
    public Boolean isPDF() {
        return ((PDFDocument) documentHandle).isPDF();
    }
}
