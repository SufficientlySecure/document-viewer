package org.ebookdroid.core.utils.archives.rar;

import org.ebookdroid.core.utils.archives.ArchiveEntry;

import java.io.IOException;
import java.io.InputStream;

import de.innosystec.unrar.rarfile.FileHeader;

public class RarArchiveEntry implements ArchiveEntry {

    final RarArchive archive;
    final FileHeader fileHeader;

    RarArchiveEntry(final RarArchive archive, final FileHeader fh) {
        this.archive = archive;
        this.fileHeader = fh;
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

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.utils.archives.ArchiveEntry#open()
     */
    @Override
    public InputStream open() throws IOException {
        return archive.open(this);
    }
}
