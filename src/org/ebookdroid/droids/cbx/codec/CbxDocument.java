package org.ebookdroid.droids.cbx.codec;

import org.ebookdroid.common.log.LogContext;
import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.core.codec.CodecPageInfo;

import android.graphics.RectF;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.emdev.utils.StringUtils;
import org.emdev.utils.archives.ArchiveEntry;
import org.emdev.utils.archives.ArchiveFile;
import org.emdev.utils.filesystem.FileExtensionFilter;

public class CbxDocument<ArchiveEntryType extends ArchiveEntry> extends AbstractCodecDocument {

    public static final LogContext LCTX = LogContext.ROOT.lctx("Cbx");

    private static final FileExtensionFilter imageFilter = new FileExtensionFilter("jpg", "jpeg", "png", "gif");

    private final ArchiveFile<ArchiveEntryType> archive;

    private final List<ArchiveEntryType> pages = new ArrayList<ArchiveEntryType>();

    /**
     * Constructor.
     *
     * @param fileName
     *            archive file name
     */
    public CbxDocument(final CbxContext<ArchiveEntryType> context, final ArchiveFile<ArchiveEntryType> archive) {
        super(context, context.getContextHandle());
        this.archive = archive;

        if (archive != null) {
            final Map<String, ArchiveEntryType> pages = new TreeMap<String, ArchiveEntryType>(StringUtils.NSC);
            final Enumeration<ArchiveEntryType> entries = archive.entries();
            while (entries.hasMoreElements()) {
                final ArchiveEntryType fh = entries.nextElement();
                if (!fh.isDirectory() && imageFilter.accept(fh)) {
                    pages.put(fh.getName().toLowerCase(), fh);
                }
            }
            this.pages.addAll(pages.values());
        }
    }

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
    public final CbxPage<ArchiveEntryType> getPage(final int pageNumber) {
        if (pageNumber < 0 || pageNumber >= getPageCount()) {
            return new CbxPage<ArchiveEntryType>(null);
        }
        return new CbxPage<ArchiveEntryType>(pages.get(pageNumber));
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecDocument#getPageInfo(int)
     */
    @Override
    public CodecPageInfo getPageInfo(final int pageIndex) {
        return archive.randomAccessAllowed() ? getPage(pageIndex).getPageInfo() : null;
    }

    @Override
    protected void freeDocument() {
        try {
            pages.clear();
            if (archive != null) {
                archive.close();
            }
        } catch (final IOException e) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("IO error: " + e.getMessage());
            }
        }
    }

    @Override
    public List<? extends RectF> searchText(final int pageNuber, final String pattern) throws DocSearchNotSupported {
        return null;
    }

}
