package org.ebookdroid.common.settings.books;

import org.ebookdroid.core.PageIndex;

public class Bookmark {

    public boolean service;
    public String name;
    public PageIndex page;
    public float offsetX;
    public float offsetY;

    public Bookmark(final String name, final PageIndex page, float offsetX, float offsetY) {
        this(false, name, page, offsetX, offsetY);
    }

    public Bookmark(final boolean service, final String name, final PageIndex page, float offsetX, float offsetY) {
        this.service = service;
        this.name = name;
        this.page = page;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public int getActualIndex(boolean splittingEnabled) {
        if (page.docIndex == page.viewIndex) {
            return page.docIndex;
        }
        return splittingEnabled ? page.viewIndex : page.docIndex;
    }

    @Override
    public String toString() {
        return name;
    }
}
