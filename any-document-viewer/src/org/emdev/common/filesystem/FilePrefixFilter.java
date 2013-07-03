package org.emdev.common.filesystem;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.emdev.utils.LengthUtils;

public class FilePrefixFilter implements FileFilter, FilenameFilter {

    private final Set<String> prefixes;

    public FilePrefixFilter(final Set<String> prefixes) {
        this.prefixes = prefixes;
    }

    public FilePrefixFilter(final String... prefixes) {
        this.prefixes = new HashSet<String>(Arrays.asList(prefixes));
    }

    @Override
    public final boolean accept(final File file) {
        return acceptImpl(file.getName().toLowerCase());
    }

    @Override
    public boolean accept(final File dir, final String name) {
        return acceptImpl(name.toLowerCase());
    }

    public boolean accept(final String name) {
        if (LengthUtils.isEmpty(name)) {
            return false;
        }
        if (!new File(name).exists()) {
            return false;
        }
        return acceptImpl(name.toLowerCase());
    }

    protected boolean acceptImpl(final String name) {
        boolean res = false;
        for (final String prefix : prefixes) {
            res |= acceptImpl(prefix, name);
        }
        return res;
    }

    protected boolean acceptImpl(final String prefix, final String name) {
        return name != null && name.startsWith(prefix);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FilePrefixFilter) {
            final FilePrefixFilter that = (FilePrefixFilter) obj;
            return this.prefixes.equals(that.prefixes);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.prefixes.hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + prefixes;
    }

}
