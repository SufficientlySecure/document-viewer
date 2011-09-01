package org.ebookdroid.xpsdroid.codec;

import org.ebookdroid.core.EBookDroidLibraryLoader;
import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;

public class XpsContext extends  AbstractCodecContext {

    static {
        EBookDroidLibraryLoader.load();
    }

    @Override
    public CodecDocument openDocument(final String fileName, final String password) {
        return new XpsDocument(this, fileName);
    }
}
