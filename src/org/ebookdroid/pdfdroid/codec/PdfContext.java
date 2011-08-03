package org.ebookdroid.pdfdroid.codec;

import org.ebookdroid.core.VuDroidLibraryLoader;
import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;

public class PdfContext extends AbstractCodecContext {

    static {
        VuDroidLibraryLoader.load();
    }

    @Override
    public CodecDocument openDocument(final String fileName, final String password) {
        return new PdfDocument(this, fileName, password);
    }
}
