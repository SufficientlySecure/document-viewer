package org.ebookdroid.core.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FileNameExtFilter implements FilenameFilter {

    private final Set<String> extensions;

    public FileNameExtFilter(final Set<String> extensions) {
        this.extensions = extensions;
    }

    public FileNameExtFilter(final String... extensions) {
        this.extensions = new HashSet<String>(Arrays.asList(extensions));
    }

    public boolean accept(File dir, String name) {
        for (final String ext : extensions) {
            if (accept(name, ext)) {
                return true;
            }
        }
        return false;
    }

    public boolean accept(String name, final String ext) {
        return name.toLowerCase().endsWith("." + ext);
    }
}
