package org.emdev.utils.filesystem;

import java.io.File;
import java.io.FileFilter;

public class DirectoryFilter implements FileFilter {

    public static final DirectoryFilter ALL = new DirectoryFilter(true);

    public static final DirectoryFilter NOT_HIDDEN = new DirectoryFilter(false);

    final boolean acceptHidden;

    private DirectoryFilter(final boolean acceptHidden) {
        this.acceptHidden = acceptHidden;
    }

    @Override
    public boolean accept(final File file) {
        return file.isDirectory() && (acceptHidden || !file.getName().startsWith("."));
    }
}
