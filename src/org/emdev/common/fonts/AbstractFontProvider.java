package org.emdev.common.fonts;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.emdev.common.fonts.data.FontPack;

public abstract class AbstractFontProvider implements IFontProvider {

    private static final AtomicInteger SEQ = new AtomicInteger();

    protected final Map<String, FontPack> packs = new TreeMap<String, FontPack>();

    public final int id;
    public final String name;

    protected AbstractFontProvider(final int id, final String name) {
        this.id = id;
        this.name = name;
    }

    public void init() {
        packs.clear();
        final List<FontPack> load = load();
        for (final FontPack fp : load) {
            packs.put(fp.name, fp);
        }
    }

    @Override
    public final int getId() {
        return id;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public int getNewPackId() {
        return SEQ.getAndIncrement();
    }

    @Override
    public Iterator<FontPack> iterator() {
        return packs.values().iterator();
    }

    @Override
    public FontPack getFontPack(final String name) {
        return packs.get(name);
    }

    protected abstract List<FontPack> load();

    protected boolean save() {
        return true;
    }

    @Override
    public final String toString() {
        return name;
    }
}
