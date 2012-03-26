package org.emdev.utils.archives;

import java.io.IOException;
import java.io.InputStream;

public interface ArchiveEntry {

    String getName();

    boolean isDirectory();

    InputStream open() throws IOException;
}
