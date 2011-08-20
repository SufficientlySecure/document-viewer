package org.ebookdroid.djvudroid.codec;

import org.ebookdroid.core.VuDroidLibraryLoader;
import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.log.LogContext;

import java.util.concurrent.Semaphore;

public class DjvuContext extends AbstractCodecContext implements Runnable {

    private static final LogContext LCTX = LogContext.ROOT.lctx("Djvu");

    static {
        VuDroidLibraryLoader.load();
    }

    private final Semaphore docSemaphore = new Semaphore(0);

    public DjvuContext() {
        super(create());
        new Thread(this).start();
    }

    @Override
    public DjvuDocument openDocument(final String fileName, final String password) {
        final DjvuDocument djvuDocument = new DjvuDocument(this, fileName);
        try {
            docSemaphore.acquire();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
        return djvuDocument;
    }

    @Override
    public void run() {
        for (;;) {
            try {
                synchronized (this) {
                    if (isRecycled()) {
                        return;
                    }
                    handleMessage(getContextHandle());
                    wait(200);
                }
            } catch (final Exception e) {
                LCTX.e("Codec error", e);
            }
        }
    }

    private void handleDocInfo() {
        docSemaphore.release();
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

    private native void handleMessage(long contextHandle);
}
