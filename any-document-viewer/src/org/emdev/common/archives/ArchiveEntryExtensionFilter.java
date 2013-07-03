package org.emdev.common.archives;

import java.util.Set;

import org.emdev.common.filesystem.FileExtensionFilter;

public class ArchiveEntryExtensionFilter extends FileExtensionFilter {

    public ArchiveEntryExtensionFilter(final Set<String> extensions) {
        super(extensions);
    }

    public ArchiveEntryExtensionFilter(final String... extensions) {
        super(extensions);
    }

    public final boolean accept(final ArchiveEntry archiveEntry) {
        return acceptImpl(archiveEntry.getName().toLowerCase());
    }
}
