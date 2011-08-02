package org.ebookdroid.core.utils.archives.rar;

import org.ebookdroid.core.utils.archives.ArchiveEntry;

import de.innosystec.unrar.rarfile.FileHeader;

public class RarArchiveEntry implements ArchiveEntry {

    private final FileHeader fileHeader;

    RarArchiveEntry(final FileHeader fh) {
        this.fileHeader = fh;
    }

    /**
     * @return RAR arhive entry header
     */
    public FileHeader getFileHeader() {
        return fileHeader;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.utils.archives.ArchiveEntry#getName()
     */
    @Override
    public String getName() {
        return fileHeader.getFileNameString();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.utils.archives.ArchiveEntry#isDirectory()
     */
    @Override
    public boolean isDirectory() {
        return fileHeader.isDirectory();
    }
}
