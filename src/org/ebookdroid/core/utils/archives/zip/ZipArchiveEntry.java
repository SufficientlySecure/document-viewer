package org.ebookdroid.core.utils.archives.zip;

import org.ebookdroid.core.utils.archives.ArchiveEntry;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

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
     * @see org.ebookdroid.core.utils.archives.ArchiveEntry#getName()
     */
    @Override
    public String getName() {
        return entry.getName();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.utils.archives.ArchiveEntry#isDirectory()
     */
    @Override
    public boolean isDirectory() {
        return entry.isDirectory();
    }

    @Override
    public InputStream open() throws IOException {
        return archive.open(this);
    }

}
