package org.ebookdroid.core.utils.archives.rar;

import org.ebookdroid.core.utils.archives.ArchiveFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import de.innosystec.unrar.Archive;
import de.innosystec.unrar.exception.RarException;
import de.innosystec.unrar.rarfile.FileHeader;

public class RarArchive implements ArchiveFile<RarArchiveEntry> {

    final Archive rarfile;

    /**
     * Constructor.
     *
     * @param file
     *            archive file
     * @throws IOException
     *             thrown on error
     */
    public RarArchive(final File file) throws IOException {
        try {
            rarfile = new Archive(file);
        } catch (final RarException ex) {
            final IOException exx = new IOException(ex.getMessage());
            exx.initCause(ex);
            throw exx;
        }
    }


    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.utils.archives.ArchiveFile#randomAccessAllowed()
     */
    @Override
    public boolean randomAccessAllowed() {
        return true;
    }


    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.utils.archives.ArchiveFile#entries()
     */
    @Override
    public Enumeration<RarArchiveEntry> entries() {
        return new Enumeration<RarArchiveEntry>() {

            private RarArchiveEntry entry;

            @Override
            public boolean hasMoreElements() {
                if (entry == null) {
                    FileHeader nextFileHeader = rarfile.nextFileHeader();
                    if (nextFileHeader != null) {
                        entry = new RarArchiveEntry(RarArchive.this, nextFileHeader);
                    }
                }
                return entry != null;
            }

            @Override
            public RarArchiveEntry nextElement() {
                final RarArchiveEntry res = entry;
                entry = null;
                return res;
            }
        };
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.utils.archives.ArchiveFile#open(org.ebookdroid.core.utils.archives.ArchiveEntry)
     */
    @Override
    public InputStream open(final RarArchiveEntry entry) throws IOException {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            rarfile.extractFile(entry.fileHeader, baos);
            baos.close();
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (final RarException ex) {
            final IOException exx = new IOException(ex.getMessage());
            exx.initCause(ex);
            throw exx;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.utils.archives.ArchiveFile#close()
     */
    @Override
    public void close() throws IOException {
        rarfile.close();
    }
}
