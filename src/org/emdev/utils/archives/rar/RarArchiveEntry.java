package org.emdev.utils.archives.rar;

import java.io.IOException;
import java.io.InputStream;

import org.emdev.utils.archives.ArchiveEntry;

public class RarArchiveEntry implements ArchiveEntry {

    final RarArchive archive;
    final String path;
    final String name;

    RarArchiveEntry(final RarArchive archive, final String path, final String name) {
        this.archive = archive;
        this.path = path;
        this.name = name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.emdev.utils.archives.ArchiveEntry#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.emdev.utils.archives.ArchiveEntry#isDirectory()
     */
    @Override
    public boolean isDirectory() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.emdev.utils.archives.ArchiveEntry#open()
     */
    @Override
    public InputStream open() throws IOException {
        final Process process = UnrarBridge.exec("p", "-inul", archive.rarfile.getAbsolutePath(), path);
        return process.getInputStream();
    }
}
