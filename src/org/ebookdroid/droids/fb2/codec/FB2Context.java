package org.ebookdroid.droids.fb2.codec;

import org.ebookdroid.EBookDroidLibraryLoader;
import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;

public class FB2Context extends AbstractCodecContext {

    static {
        EBookDroidLibraryLoader.load();
    }

    @Override
    public CodecDocument openDocument(final String fileName, final String password) {
        return new FB2Document(fileName);
    }

    @Override
    public boolean isPageSizeCacheable() {
        return false;
    }
}
