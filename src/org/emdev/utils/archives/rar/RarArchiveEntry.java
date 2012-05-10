package org.emdev.utils.archives.rar;

import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.log.LogContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.emdev.utils.archives.ArchiveEntry;

public class RarArchiveEntry implements ArchiveEntry {

    private static final LogContext LCTX = LogContext.ROOT.lctx("Unrar");

    final RarArchive archive;
    final String path;
    final String name;

    private File cached;

    RarArchiveEntry(final RarArchive archive, final String path, final String name) {
        this.archive = archive;
        this.path = path;
        this.name = name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.emdev.utils.archives.ArchiveEntry#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.emdev.utils.archives.ArchiveEntry#isDirectory()
     */
    @Override
    public boolean isDirectory() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.emdev.utils.archives.ArchiveEntry#open()
     */
    @Override
    public InputStream open() throws IOException {
        if (cached == null || !cached.exists()) {
            final Process process = UnrarBridge.exec("p", "-inul", archive.rarfile.getAbsolutePath(), path);
            final InputStream inputStream = process.getInputStream();
            final InputStream errorStream = process.getErrorStream();

            new Thread(new Runnable() {

                @Override
                public void run() {
                    BufferedReader err = new BufferedReader(new InputStreamReader(errorStream), 8 * 1024);
                    try {
                        for (String s = err.readLine(); s != null; s = err.readLine()) {
                            LCTX.e(s);
                        }
                    } catch (IOException ex) {
                    }
                    try {
                        err.close();
                    } catch (IOException ex) {
                    }
                }
            }).start();

            cached = CacheManager.createTempFile(inputStream, "page");
        }
        FileInputStream tempin = new FileInputStream(cached);
        return tempin;
    }
}
