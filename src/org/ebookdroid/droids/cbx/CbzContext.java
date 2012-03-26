package org.ebookdroid.droids.cbx;

import org.ebookdroid.droids.cbx.codec.CbxContext;

import java.io.File;
import java.io.IOException;

import org.emdev.utils.archives.ArchiveFile;
import org.emdev.utils.archives.zip.ZipArchive;
import org.emdev.utils.archives.zip.ZipArchiveEntry;

public class CbzContext extends CbxContext<ZipArchiveEntry> {

    public CbzContext() {
        super();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.droids.cbx.codec.CbxArchiveFactory#createArchive(java.io.File, java.lang.String)
     */
    @Override
    public ArchiveFile<ZipArchiveEntry> createArchive(final File file, final String password) throws IOException {
        return new ZipArchive(file);
    }

}
