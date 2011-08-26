package org.ebookdroid.core;


public class Bookmark {

    final int page;
    final String name;
    final boolean service;

    public Bookmark(final int page, final String name) {
        this(page, name, false);
    }

    public Bookmark(final int page, final String name, final boolean service) {
        this.page = page;
        this.name = name;
        this.service = service;
    }

    public int getPage() {
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
