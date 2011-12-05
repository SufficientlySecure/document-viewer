package org.ebookdroid.core.presentation;

public class BookNode {

    final int listNum;
    final String name;
    final String path;

    BookNode(final int listNum, final String name, final String path) {
        this.listNum = listNum;
        this.name = name;
        this.path = path;
    }

    String getName() {
        return this.name;
    }

    public String getPath() {
        return this.path;
    }

    @Override
    public String toString() {
        return this.name;
    }
}