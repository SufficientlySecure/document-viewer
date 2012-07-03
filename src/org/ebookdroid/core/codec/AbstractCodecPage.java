package org.ebookdroid.core.codec;

import android.graphics.RectF;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractCodecPage implements CodecPage {

    private final AtomicBoolean lock = new AtomicBoolean();

    @Override
    public final void lock() {
        lock.set(true);
    }

    @Override
    public final boolean locked() {
        return lock.get();
    }

    @Override
    public final void unlock() {
        lock.set(false);
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
