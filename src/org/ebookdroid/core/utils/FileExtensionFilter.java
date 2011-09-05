package org.ebookdroid.core.utils;

import org.ebookdroid.core.utils.archives.ArchiveEntry;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
        return name.toLowerCase().endsWith("." + ext);
    }
}
