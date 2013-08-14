package org.emdev.common.filesystem;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.emdev.utils.LengthUtils;

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
        for (final String ext : extensions) {
            if (acceptImpl(name, ext)) {
                return true;
            }
        }
        return false;
    }
    
    protected boolean acceptImpl(final String name, final String ext) {
        return name != null && name.endsWith("." + ext);
    }

    public String[] list(File folder) {
        return folder.list(this);
    }

    public File[] listFiles(File folder) {
        return folder.listFiles((FilenameFilter) this);
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

    public String toString() {
        return this.getClass().getSimpleName() + extensions;
    }
}
