package org.ebookdroid.droids.djvu.codec;

import org.ebookdroid.EBookDroidLibraryLoader;
import org.ebookdroid.common.log.LogContext;
import org.ebookdroid.core.codec.AbstractCodecContext;

public class DjvuContext extends AbstractCodecContext {

    private static final LogContext LCTX = LogContext.ROOT.lctx("Djvu");

    static {
        EBookDroidLibraryLoader.load();
    }

    public DjvuContext() {
        super(create());
    }

    @Override
    public DjvuDocument openDocument(final String fileName, final String password) {
        return new DjvuDocument(this, fileName);
    }

    @Override
    protected void freeContext() {
        try {
            free(getContextHandle());
        } catch (Throwable th) {
        }
    }

    private static native long create();

    private static native void free(long contextHandle);
}
