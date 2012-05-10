package org.emdev.utils.archives.rar;

import org.ebookdroid.common.log.LogContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.emdev.utils.LengthUtils;
import org.emdev.utils.archives.ArchiveFile;

public class RarArchive implements ArchiveFile<RarArchiveEntry> {

    private static final LogContext LCTX = LogContext.ROOT.lctx("Unrar");

    final File rarfile;
    final List<RarArchiveEntry> entries = new LinkedList<RarArchiveEntry>();

    /**
     * Constructor.
     * 
     * @param file
     *            archive file
     * @throws IOException
     *             thrown on error
     */
    public RarArchive(final File file) throws IOException {
        rarfile = file;

        final Set<String> dirs = new HashSet<String>();

        final Process process = UnrarBridge.exec("vb", rarfile.getAbsolutePath());
        final BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()), 8 * 1024);

        LCTX.i("List archive entries for: " + rarfile.getAbsolutePath());
        for (String s = in.readLine(); s != null; s = in.readLine()) {
            if (dirs.contains(s)) {
                continue;
            }
            LCTX.i("Entry: " + s);
            final File f = new File(s);
            entries.add(new RarArchiveEntry(this, s, f.getName()));

            String dir = f.getParent();
            while (LengthUtils.isNotEmpty(dir)) {
                dirs.add(dir);
                dir = new File(dir).getParent();
            }

        }
        try {
            process.waitFor();
        } catch (final InterruptedException ex) {
            Thread.interrupted();
            throw new IOException(ex.getMessage());
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.emdev.utils.archives.ArchiveFile#randomAccessAllowed()
     */
    @Override
    public boolean randomAccessAllowed() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.emdev.utils.archives.ArchiveFile#entries()
     */
    @Override
    public Enumeration<RarArchiveEntry> entries() {
        return Collections.enumeration(entries);
    }

    @Override
    public void close() throws IOException {
    }
}
