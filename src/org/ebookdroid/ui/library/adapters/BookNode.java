package org.ebookdroid.ui.library.adapters;


import java.io.File;

import org.emdev.utils.StringUtils;

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
