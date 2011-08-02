package org.ebookdroid.cbdroid.codec;

import org.ebookdroid.core.OutlineLink;
import org.ebookdroid.core.PageLink;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.utils.FileExtensionFilter;
import org.ebookdroid.core.utils.archives.ArchiveEntry;
import org.ebookdroid.core.utils.archives.ArchiveFile;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public abstract class AbsrtractCbxDocument<ArchiveType extends ArchiveFile<ArchiveEntryType>, ArchiveEntryType extends ArchiveEntry>
        implements CodecDocument {

    private static final FileExtensionFilter imageFilter = new FileExtensionFilter("jpg", "jpeg", "png");

    private final String LCTX = getClass().getSimpleName();

    private ArchiveType archive;

    private final List<ArchiveEntryType> pages = new ArrayList<ArchiveEntryType>();

    /**
     * Constructor.
     *
     * @param fileName
     *            archive file name
     */
    public AbsrtractCbxDocument(final String fileName) {
        try {
            archive = createArchive(new File(fileName));
            if (archive != null) {
                Map<String, ArchiveEntryType> pages = new TreeMap<String, ArchiveEntryType>();
                final Enumeration<ArchiveEntryType> entries = archive.entries();
                while (entries.hasMoreElements()) {
                    ArchiveEntryType fh = entries.nextElement();
                    if (!fh.isDirectory() && imageFilter.accept(fh)) {
                        pages.put(fh.getName().toLowerCase(), fh);
                    }
                }
                this.pages.addAll(pages.values());
            }
        } catch (final IOException e) {
            Log.d(LCTX, "IO error: " + e.getMessage());
        }
    }

    /**
     * Creates archive wrapper instance for the given file.
     *
     * @param file
     *            archive file
     * @return an instance of the archive wrapper
     * @throws IOException
     *             thrown on error
     */
    protected abstract ArchiveType createArchive(final File file) throws IOException;

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecDocument#getPageCount()
     */
    @Override
    public final int getPageCount() {
        return pages.size();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecDocument#getPage(int)
     */
    @Override
    public final CbxPage<ArchiveType, ArchiveEntryType> getPage(final int pageNumber) {
        if (pageNumber < 0 || pageNumber >= getPageCount()) {
            return new CbxPage<ArchiveType, ArchiveEntryType>(null, null);
        }
        return new CbxPage<ArchiveType, ArchiveEntryType>(archive, pages.get(pageNumber));
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecDocument#getOutline()
     */
    @Override
    public List<OutlineLink> getOutline() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecDocument#getPageLinks(int)
     */
    @Override
    public List<PageLink> getPageLinks(final int pageNuber) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecDocument#recycle()
     */
    @Override
    public final void recycle() {
        try {
            pages.clear();
            archive.close();
        } catch (final IOException e) {
            Log.d(LCTX, "IO error: " + e.getMessage());
        }
    }
}
