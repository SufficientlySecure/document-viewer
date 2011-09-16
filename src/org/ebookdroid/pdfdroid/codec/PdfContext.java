package org.ebookdroid.pdfdroid.codec;

import org.ebookdroid.core.EBookDroidLibraryLoader;
import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;

public class PdfContext extends AbstractCodecContext {

    static {
        EBookDroidLibraryLoader.load();
    }

    @Override
    public CodecDocument openDocument(final String fileName, final String password) {
        return new PdfDocument(this, fileName, password);
    }

    @Override
    public boolean isPageSizeCacheable() {
        return true;
    }
}
