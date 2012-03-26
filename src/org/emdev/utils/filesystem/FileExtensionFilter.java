package org.emdev.utils.filesystem;


import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.emdev.utils.archives.ArchiveEntry;

public class FileExtensionFilter implements FileFilter, FilenameFilter {

    private final Set<String> extensions;

    public FileExtensionFilter(final Set<String> extensions) {
        this.extensions = extensions;
    }

    public FileExtensionFilter(final String... extensions) {
        this.extensions = new HashSet<String>(Arrays.asList(extensions));
    }

    @Override
    public final boolean accept(final File file) {
        for (final String ext : extensions) {
            if (accept(file.getName(), ext)) {
                return true;
            }
        }
        return false;
    }

    public final boolean accept(final ArchiveEntry archiveEntry) {
        for (final String ext : extensions) {
            if (accept(archiveEntry.getName(), ext)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean accept(final File dir, final String name) {
        for (final String ext : extensions) {
            if (accept(name, ext)) {
                return true;
            }
        }
        return false;
    }

    public boolean accept(final String name) {
        for (final String ext : extensions) {
            if (accept(name, ext) && new File(name).exists()) {
                return true;
            }
        }
        return false;
    }

    public boolean accept(final String name, final String ext) {
        return name != null && name.toLowerCase().endsWith("." + ext);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FileExtensionFilter) {
            final FileExtensionFilter that = (FileExtensionFilter) obj;
            return this.extensions.equals(that.extensions);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.extensions.hashCode();
    }
}
