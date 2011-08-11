package org.ebookdroid.core.utils;

import java.io.File;
import java.io.FileFilter;

public class DirectoryOrFileFilter implements FileFilter {

    private final FileFilter fileFilter;

    public DirectoryOrFileFilter(FileFilter fileFilter) {
        this.fileFilter = fileFilter;
    }

    @Override
    public boolean accept(File file) {
        return file.isDirectory() || fileFilter.accept(file);
    }
}