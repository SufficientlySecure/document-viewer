package org.ebookdroid.core.codec;

import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class CodecPageHolder {

    private final AtomicLong lock = new AtomicLong();

    private final AtomicBoolean access = new AtomicBoolean();

    private final CodecDocument document;
    private final int pageIndex;

    private SoftReference<CodecPage> ref = new SoftReference<CodecPage>(null);

    public CodecPageHolder(final CodecDocument document, final int pageIndex) {
        this.document = document;
        this.pageIndex = pageIndex;
    }

    public CodecPage getPage() {
        while (true) {
            if (access.compareAndSet(false, true)) {
                CodecPage page = ref.get();
                try {
                    if (page == null || page.isRecycled()) {
                        page = null;
                        page = document.getPage(pageIndex);
                        return page;
                    }
                } finally {
                    if (page != null) {
                        lock();
                        ref = new SoftReference<CodecPage>(page);
                    }
                    access.set(false);
                    synchronized (access) {
                        access.notifyAll();
                    }
                }
            } else {
                synchronized (access) {
                    try {
                        access.wait();
                    } catch (final InterruptedException ex) {
                        Thread.interrupted();
                    }
                }
            }
        }
    }

    public boolean isInvalid() {
        while (true) {
            if (access.compareAndSet(false, true)) {
                try {
                    final CodecPage page = ref.get();
                    return page == null || page.isRecycled();
                } finally {
                    access.set(false);
                    synchronized (access) {
                        access.notifyAll();
                    }
                }
            } else {
                synchronized (access) {
                    try {
                        access.wait();
                    } catch (final InterruptedException ex) {
                        Thread.interrupted();
                    }
                }
            }
        }
    }

    public boolean recycle(final boolean shutdown) {
        while (true) {
            if (shutdown || access.compareAndSet(false, true)) {
                if (!shutdown && locked()) {
                    return false;
                }
                try {
                    final CodecPage page = ref.get();
                    if (page != null && !page.isRecycled()) {
                        page.recycle();
                    }
                    return true;
                } finally {
                    access.set(false);
                    synchronized (access) {
                        access.notifyAll();
                    }
                }
            } else {
                synchronized (access) {
                    try {
                        access.wait();
                    } catch (final InterruptedException ex) {
                        Thread.interrupted();
                    }
                }
            }
        }
    }

    public final void lock() {
        lock.incrementAndGet();
    }

    public final boolean locked() {
        return lock.get() > 0;
    }

    public final void unlock() {
        lock.decrementAndGet();
    }
}
