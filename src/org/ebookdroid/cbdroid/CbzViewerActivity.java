package org.ebookdroid.cbdroid;

import org.ebookdroid.cbdroid.codec.CbxArchiveFactory;
import org.ebookdroid.cbdroid.codec.CbxContext;
import org.ebookdroid.core.BaseViewerActivity;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.DecodeServiceBase;
import org.ebookdroid.core.utils.archives.ArchiveFile;
import org.ebookdroid.core.utils.archives.zip.ZipArchive;
import org.ebookdroid.core.utils.archives.zip.ZipArchiveEntry;

import java.io.File;
import java.io.IOException;

public class CbzViewerActivity extends BaseViewerActivity implements CbxArchiveFactory<ZipArchiveEntry> {

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.BaseViewerActivity#createDecodeService()
     */
    @Override
    protected DecodeService createDecodeService() {
        return new DecodeServiceBase(new CbxContext<ZipArchiveEntry>(this));
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.cbdroid.codec.CbxArchiveFactory#create(java.io.File, java.lang.String)
     */
    @Override
    public ArchiveFile<ZipArchiveEntry> create(final File file, final String password) throws IOException {
        return new ZipArchive(file);
    }

}
