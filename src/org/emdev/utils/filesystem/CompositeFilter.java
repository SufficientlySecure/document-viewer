package org.emdev.utils.filesystem;

import java.io.File;
import java.io.FileFilter;

public class CompositeFilter implements FileFilter {

    final boolean acceptAll;

    final FileFilter[] fileFilters;

    public CompositeFilter(final boolean acceptAll, final FileFilter... fileFilters) {
        this.acceptAll = acceptAll;
        this.fileFilters = fileFilters;
    }

    @Override
    public boolean accept(final File file) {
        boolean res = false;
        if (acceptAll) {
            for (final FileFilter f : fileFilters) {
                res &= f.accept(file);
                if (!res) {
                    break;
                }
            }
        } else {
            for (final FileFilter f : fileFilters) {
                res |= f.accept(file);
                if (res) {
                    break;
                }
            }
        }
        return res;
    }
}
