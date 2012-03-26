package org.emdev.utils.archives;

import java.io.Closeable;
import java.util.Enumeration;

public interface ArchiveFile<ArchiveEntryType extends ArchiveEntry> extends Closeable {

    boolean randomAccessAllowed();

    Enumeration<ArchiveEntryType> entries();
}
