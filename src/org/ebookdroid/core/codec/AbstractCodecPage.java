package org.ebookdroid.core.codec;

import android.graphics.RectF;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractCodecPage implements CodecPage {

    private final AtomicLong lock = new AtomicLong();

    @Override
    public final void lock() {
        lock.incrementAndGet();
    }

    @Override
    public final boolean locked() {
        return lock.get() > 0;
    }

    @Override
    public final void unlock() {
        lock.decrementAndGet();
    }

    @Override
    public List<PageLink> getPageLinks() {
        return Collections.emptyList();
    }

    @Override
    public List<PageTextBox> getPageText() {
        return Collections.emptyList();
    }

    @Override
    public List<? extends RectF> searchText(String pattern) {
        return Collections.emptyList();
    }
}
