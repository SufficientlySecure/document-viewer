package org.ebookdroid.droids.cbx.codec;


import java.io.File;
import java.io.IOException;

import org.emdev.utils.archives.ArchiveEntry;
import org.emdev.utils.archives.ArchiveFile;

public interface CbxArchiveFactory<ArchiveEntryType extends ArchiveEntry> {

    ArchiveFile<ArchiveEntryType> createArchive(final File file, final String password) throws IOException;
}
