package org.ebookdroid.ui.library.adapters;


import org.ebookdroid.common.settings.books.BookSettings;

import java.io.File;

import org.emdev.utils.StringUtils;

public class BookNode implements Comparable<BookNode> {

    public final String name;
    public final String path;
    public BookSettings settings;

    BookNode(final File f, BookSettings settings) {
        this.name = f.getName();
        this.path = f.getAbsolutePath();
        this.settings = settings;
    }

    BookNode(BookSettings settings) {
        File f = new File(settings.fileName);
        this.name = f.getName();
        this.path = f.getAbsolutePath();
        this.settings = settings;
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
