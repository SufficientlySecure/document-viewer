package org.ebookdroid.pdfdroid.codec;

import org.ebookdroid.core.VuDroidLibraryLoader;
import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;

public class PdfContext extends AbstractCodecContext {

    static {
        VuDroidLibraryLoader.load();
    }

    public CodecDocument openDocument(String fileName, String password) {
        return PdfDocument.openDocument(fileName, password);
    }
}
