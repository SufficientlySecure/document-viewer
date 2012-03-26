package org.ebookdroid.droids.cbx;

import org.ebookdroid.droids.cbx.codec.CbxContext;

import java.io.File;
import java.io.IOException;

import org.emdev.utils.archives.ArchiveFile;
import org.emdev.utils.archives.rar.RarArchive;
import org.emdev.utils.archives.rar.RarArchiveEntry;

public class CbrContext extends CbxContext<RarArchiveEntry> {

    public CbrContext(){
        super();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.droids.cbx.codec.CbxArchiveFactory#createArchive(java.io.File, java.lang.String)
     */
    @Override
    public ArchiveFile<RarArchiveEntry> createArchive(final File file, final String password) throws IOException {
        return new RarArchive(file);
    }

}
