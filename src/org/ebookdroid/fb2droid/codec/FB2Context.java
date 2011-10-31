package org.ebookdroid.fb2droid.codec;

import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;

public class FB2Context extends AbstractCodecContext {

    @Override
    public CodecDocument openDocument(final String fileName, final String password) {
        return new FB2Document(fileName);
    }

    @Override
    public boolean isPageSizeCacheable() {
        return false;
    }
}
