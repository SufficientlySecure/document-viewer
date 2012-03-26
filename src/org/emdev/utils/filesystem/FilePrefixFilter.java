package org.emdev.utils.filesystem;


import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.emdev.utils.archives.ArchiveEntry;

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
        for (final String prefix : prefixes) {
            if (accept(prefix, file.getName())) {
                return true;
            }
        }
        return false;
    }

    public final boolean accept(final ArchiveEntry archiveEntry) {
        for (final String prefix : prefixes) {
            if (accept(prefix, archiveEntry.getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean accept(final File dir, final String name) {
        for (final String prefix : prefixes) {
            if (accept(prefix, name)) {
                return true;
            }
        }
        return false;
    }

    public boolean accept(final String name) {
        for (final String prefix : prefixes) {
            if (accept(prefix, name) && new File(name).exists()) {
                return true;
            }
        }
        return false;
    }

    public boolean accept(final String prefix, final String name) {
        return name != null && name.toLowerCase().startsWith(prefix);
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
}
