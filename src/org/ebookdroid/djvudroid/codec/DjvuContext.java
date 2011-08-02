package org.ebookdroid.djvudroid.codec;

import org.ebookdroid.core.VuDroidLibraryLoader;
import org.ebookdroid.core.codec.AbstractCodecContext;

import android.util.Log;

import java.util.concurrent.Semaphore;

public class DjvuContext extends AbstractCodecContext implements Runnable {

    static {
        VuDroidLibraryLoader.load();
    }

    private static final String DJVU_DROID_CODEC_LIBRARY = "DjvuDroidCodecLibrary";
    private final Semaphore docSemaphore = new Semaphore(0);

    public DjvuContext() {
        super(create());
        new Thread(this).start();
    }

    @Override
    public DjvuDocument openDocument(final String fileName, final String password) {
        final DjvuDocument djvuDocument = DjvuDocument.openDocument(fileName, this);
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
                Log.e(DJVU_DROID_CODEC_LIBRARY, "Codec error", e);
            }
        }
    }

    private void handleDocInfo() {
        docSemaphore.release();
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    @Override
    public synchronized void recycle() {
        if (isRecycled()) {
            return;
        }
        try {
            free(getContextHandle());
        } finally {
            super.recycle();
        }
    }

    private static native long create();

    private static native void free(long contextHandle);

    private native void handleMessage(long contextHandle);
}
