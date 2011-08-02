package org.ebookdroid.cbdroid;

import org.ebookdroid.cbdroid.codec.CbxArchiveFactory;
import org.ebookdroid.cbdroid.codec.CbxContext;
import org.ebookdroid.core.BaseViewerActivity;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.DecodeServiceBase;
import org.ebookdroid.core.utils.archives.ArchiveFile;
import org.ebookdroid.core.utils.archives.rar.RarArchive;
import org.ebookdroid.core.utils.archives.rar.RarArchiveEntry;

import java.io.File;
import java.io.IOException;

public class CbrViewerActivity extends BaseViewerActivity implements CbxArchiveFactory<RarArchiveEntry> {

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.BaseViewerActivity#createDecodeService()
     */
    @Override
    protected DecodeService createDecodeService() {
        return new DecodeServiceBase(new CbxContext<RarArchiveEntry>(this));
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.cbdroid.codec.CbxArchiveFactory#create(java.io.File, java.lang.String)
     */
    @Override
    public ArchiveFile<RarArchiveEntry> create(final File file, final String password) throws IOException {
        return new RarArchive(file);
    }

}
