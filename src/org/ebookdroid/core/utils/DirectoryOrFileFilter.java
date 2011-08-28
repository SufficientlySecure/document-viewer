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
//TODO: Make show hidden files configurable.
        return (file.isDirectory() && !file.getName().startsWith(".")) || fileFilter.accept(file);
                
    }
}