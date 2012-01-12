package org.ebookdroid.core.presentation;

import org.ebookdroid.utils.StringUtils;

import java.io.File;

public class BookNode implements Comparable<BookNode> {

    public final String name;
    public final String path;

    BookNode(final File f) {
        this.name = f.getName();
        this.path = f.getAbsolutePath();
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
