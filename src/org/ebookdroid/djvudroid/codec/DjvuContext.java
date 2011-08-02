package org.ebookdroid.djvudroid.codec;

import org.ebookdroid.core.VuDroidLibraryLoader;
import org.ebookdroid.core.codec.CodecContext;

import android.content.ContentResolver;
import android.util.Log;

import java.util.concurrent.Semaphore;

public class DjvuContext implements Runnable, CodecContext {

    static {
        VuDroidLibraryLoader.load();
    }

    private long contextHandle;
    private static final String DJVU_DROID_CODEC_LIBRARY = "DjvuDroidCodecLibrary";
    private final Semaphore docSemaphore = new Semaphore(0);

    public DjvuContext() {
        this.contextHandle = create();
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
    public long getContextHandle() {
        return contextHandle;
    }

    @Override
    public void run() {
        for (;;) {
            try {
                synchronized (this) {
                    if (isRecycled()) {
                        return;
                    }
                    handleMessage(contextHandle);
                    wait(200);
                }
            } catch (final Exception e) {
                Log.e(DJVU_DROID_CODEC_LIBRARY, "Codec error", e);
            }
        }
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    private void handleDocInfo() {
        docSemaphore.release();
    }

    @Override
    public void setContentResolver(final ContentResolver contentResolver) {
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
        free(contextHandle);
        contextHandle = 0;
    }

    private boolean isRecycled() {
        return contextHandle == 0;
    }

    private static native long create();

    private static native void free(long contextHandle);

    private native void handleMessage(long contextHandle);
}
