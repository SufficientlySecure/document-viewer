package org.ebookdroid.cbdroid.codec;

import org.ebookdroid.core.utils.archives.ArchiveEntry;
import org.ebookdroid.core.utils.archives.ArchiveFile;

import java.io.File;
import java.io.IOException;

public interface CbxArchiveFactory<ArchiveEntryType extends ArchiveEntry> {

    ArchiveFile<ArchiveEntryType> create(final File file, final String password) throws IOException;
}
