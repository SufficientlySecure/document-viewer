package org.ebookdroid.core.presentation;

import org.ebookdroid.utils.StringUtils;

public class BookNode implements Comparable<BookNode>{

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

    @Override
    public int compareTo(BookNode that) {
        if (this == that) {
            return 0;
        }

        return StringUtils.compareNatural(this.path, that.path);
    }
}