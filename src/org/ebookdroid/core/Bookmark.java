package org.ebookdroid.core;

public class Bookmark {

    private final int page;
    private final String name;

    public Bookmark(int page, String name) {
        super();
        this.page = page;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public int getPage() {
        return page;
    }
}