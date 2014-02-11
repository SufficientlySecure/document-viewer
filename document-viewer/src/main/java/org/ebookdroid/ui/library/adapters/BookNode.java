package org.ebookdroid.ui.library.adapters;

import org.ebookdroid.common.settings.books.BookSettings;

import java.io.File;

import org.emdev.utils.FileUtils;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.StringUtils;

public class BookNode implements Comparable<BookNode> {

    public final String name;
    public final String path;
    public final String mpath;
    public BookSettings settings;

    public BookNode(final File f, final BookSettings settings) {
        this.name = f.getName();
        this.path = f.getAbsolutePath();
        this.mpath = FileUtils.invertMountPrefix(this.path);
        this.settings = settings;
    }

    public BookNode(final BookSettings settings) {
        final File f = new File(settings.fileName);
        this.name = f.getName();
        this.path = f.getAbsolutePath();
        this.mpath = FileUtils.invertMountPrefix(this.path);
        this.settings = settings;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BookNode) {
            return 0 == compareTo((BookNode) obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return LengthUtils.safeString(this.path).hashCode();
    }

    @Override
    public int compareTo(final BookNode that) {
        if (this == that) {
            return 0;
        }
        return StringUtils.compareNatural(this.path, that.path);
    }
}
