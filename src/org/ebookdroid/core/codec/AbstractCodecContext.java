package org.ebookdroid.core.codec;

import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractCodecContext implements CodecContext {

    private static final AtomicLong SEQ = new AtomicLong();

    private long contextHandle;

    /**
     * Constructor.
     */
    protected AbstractCodecContext() {
        this(SEQ.incrementAndGet());
    }

    /**
     * Constructor.
     *
     * @param contextHandle
     *            contect handler
     */
    protected AbstractCodecContext(final long contextHandle) {
        this.contextHandle = contextHandle;
    }

    @Override
    protected final void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecContext#recycle()
     */
    @Override
    public final void recycle() {
        if (!isRecycled()) {
            freeContext();
            contextHandle = 0;
        }
    }

    protected void freeContext() {
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecContext#isRecycled()
     */
    @Override
    public final boolean isRecycled() {
        return contextHandle == 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecContext#getContextHandle()
     */
    @Override
    public final long getContextHandle() {
        return contextHandle;
    }

}
