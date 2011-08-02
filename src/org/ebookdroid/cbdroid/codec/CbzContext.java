package org.ebookdroid.cbdroid.codec;

import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;

public class CbzContext extends AbstractCodecContext {

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecContext#openDocument(java.lang.String, java.lang.String)
     */
    @Override
    public CodecDocument openDocument(final String fileName, final String password) {
        return CbzDocument.openDocument(fileName);
    }
}
