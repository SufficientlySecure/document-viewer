package org.ebookdroid.core.utils.archives;

import java.io.IOException;
import java.io.InputStream;

public interface ArchiveEntry {

    String getName();

    boolean isDirectory();

    InputStream open() throws IOException;
}
