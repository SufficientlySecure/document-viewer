package org.ebookdroid.droids.djvu.codec;

import org.ebookdroid.EBookDroidLibraryLoader;
import org.ebookdroid.core.codec.AbstractCodecContext;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;

public class DjvuContext extends AbstractCodecContext {

    private static final LogContext LCTX = LogManager.root().lctx("Djvu");

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
