package org.ebookdroid.core.settings;

import org.ebookdroid.core.PageIndex;

public class Bookmark {

    PageIndex page;
    String name;
    boolean service;

    public Bookmark(final PageIndex page, final String name) {
        this(page, name, false);
    }

    public Bookmark(final PageIndex page, final String name, final boolean service) {
        this.page = page;
        this.name = name;
        this.service = service;
    }

    public int getActualIndex(boolean splittingEnabled) {
        if (page.docIndex == page.viewIndex) {
            return page.docIndex;
        }
        return splittingEnabled ? page.viewIndex : page.docIndex;
    }

    public PageIndex getPage() {
        return page;
    }

    public String getName() {
        return name;
    }

    public boolean isService() {
        return service;
    }

    @Override
    public String toString() {
        return name;
    }
}
