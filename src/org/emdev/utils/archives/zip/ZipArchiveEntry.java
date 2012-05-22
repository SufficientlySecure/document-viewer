package org.emdev.utils.archives.zip;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

import org.emdev.utils.archives.ArchiveEntry;

public class ZipArchiveEntry implements ArchiveEntry {

    final ZipArchive archive;
    final ZipEntry entry;

    ZipArchiveEntry(final ZipArchive archive, final ZipEntry entry) {
        this.archive = archive;
        this.entry = entry;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.utils.archives.ArchiveEntry#getName()
     */
    @Override
    public String getName() {
        return entry.getName();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.utils.archives.ArchiveEntry#isDirectory()
     */
    @Override
    public boolean isDirectory() {
        return entry.isDirectory();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.utils.archives.ArchiveEntry#open()
     */
    @Override
    public InputStream open() throws IOException {
        return archive.open(this);
    }

    public long getCompressedSize() {
        return entry.getCompressedSize();
    }

    public long getSize() {
        return entry.getSize();
    }

}
