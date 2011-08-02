package org.ebookdroid.core.utils.archives;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

public interface ArchiveFile<ArchiveEntryType extends ArchiveEntry> {

    boolean randomAccessAllowed();

    Enumeration<ArchiveEntryType> entries();

    InputStream open(ArchiveEntryType entry) throws IOException;

    void close() throws IOException;
}
