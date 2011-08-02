package org.ebookdroid.core.utils.archives.zip;

import org.ebookdroid.core.utils.archives.ArchiveEntry;

import java.util.zip.ZipEntry;

public class ZipArchiveEntry implements ArchiveEntry {

    final ZipEntry entry;

    ZipArchiveEntry(final ZipEntry entry) {
        this.entry = entry;
    }

    /**
     * @return ZIP archive entry
     */
    public ZipEntry getEntry() {
        return entry;
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

}
