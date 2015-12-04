package org.emdev.common.cache;

import android.content.Context;
import android.net.Uri;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.emdev.common.filesystem.FilePrefixFilter;
import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.ui.progress.IProgressIndicator;
import org.emdev.ui.progress.UIFileCopying;
import org.emdev.utils.FileUtils;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.StringUtils;

public class CacheManager {

    public static final LogContext LCTX = LogManager.root().lctx("CacheManager", false);

    protected static Context s_context;

    protected static File s_cacheDir;

    public static void init(final Context context) {
        s_context = context;
        s_cacheDir = context.getFilesDir();
        LCTX.i("Default app cache dir: " + FileUtils.getAbsolutePath(s_cacheDir));
    }

    public static File getCacheDir() {
        return s_cacheDir;
    }

    public static boolean setCacheDir(final File newCache, final boolean moveFiles, final IProgressIndicator progress) {
        final File oldCache = s_cacheDir;
        s_cacheDir = newCache;

        if (s_cacheDir == null || s_cacheDir.equals(oldCache)) {
            return false;
        }

        s_cacheDir.mkdir();
        LCTX.i("Actual app cache dir: " + s_cacheDir.getAbsolutePath());

        if (!moveFiles) {
            return true;
        }

        final String[] files = oldCache.list();
        if (LengthUtils.isEmpty(files)) {
            return true;
        }

        final int count = FileUtils.move(oldCache, newCache, files, progress);
        LCTX.i("" + count + " files moved");
        return true;
    }

    public static File createTempFile(final InputStream source, final String suffix) throws IOException {
        final File tempfile = File.createTempFile("temp", suffix, s_cacheDir);
        tempfile.deleteOnExit();

        FileUtils.copy(source, new FileOutputStream(tempfile));

        return tempfile;
    }

    public static File createTempDocument(final InputStream source, final String fileName, final UIFileCopying worker)
            throws IOException {
        final File tempfile = new File(s_cacheDir, fileName);
        tempfile.deleteOnExit();

        worker.copy(-1, source, new FileOutputStream(tempfile));
        return tempfile;
    }

    public static File createTempFile(final byte[] source, final String suffix) throws IOException {
        final File tempfile = File.createTempFile("temp", suffix, s_cacheDir);
        tempfile.deleteOnExit();

        FileUtils.copy(new ByteArrayInputStream(source), new FileOutputStream(tempfile), source.length, null);

        return tempfile;
    }

    public static File createTempFile(final Uri uri, final String extension, final UIFileCopying worker) throws IOException {
        final File tempfile = File.createTempFile("temp", "content." + extension, s_cacheDir);
        tempfile.deleteOnExit();

        final InputStream source = s_context.getContentResolver().openInputStream(uri);
        worker.copy(-1, source, new FileOutputStream(tempfile));

        return tempfile;
    }

    public static File createTempFile(final Uri uri) throws IOException {
        final File tempfile = File.createTempFile("temp", "content", s_cacheDir);
        tempfile.deleteOnExit();

        final InputStream source = s_context.getContentResolver().openInputStream(uri);
        FileUtils.copy(source, new FileOutputStream(tempfile));

        return tempfile;
    }

    public static void clear() {
        final String[] files = s_cacheDir != null ? s_cacheDir.list() : null;
        if (LengthUtils.isNotEmpty(files)) {
            for (final String file : files) {
                new File(s_cacheDir, file).delete();
            }
        }
    }

    public static void copy(final String sourcePath, final String targetPath, final boolean deleteSource) {
        final FilePrefixFilter filter = new FilePrefixFilter(StringUtils.md5(sourcePath) + ".");
        final String[] files = s_cacheDir != null ? s_cacheDir.list(filter) : null;
        if (LengthUtils.isEmpty(files)) {
            return;
        }
        final String targetPrefix = StringUtils.md5(targetPath);
        boolean renamed = true;
        for (final String file : files) {
            final File source = new File(s_cacheDir, file);
            final File target = new File(s_cacheDir, targetPrefix + FileUtils.getExtensionWithDot(source));
            if (deleteSource) {
                renamed = renamed && source.renameTo(target);
                if (!renamed) {
                    try {
                        FileUtils.copy(source, target);
                        source.delete();
                        if (LCTX.isDebugEnabled()) {
                            LCTX.d("File moving completed: " + target.getName());
                        }
                    } catch (final IOException ex) {
                        LCTX.e("File moving failed: " + ex.getMessage());
                    }
                } else {
                    if (LCTX.isDebugEnabled()) {
                        LCTX.d("File renaming completed: " + target.getName());
                    }
                }
            } else {
                try {
                    FileUtils.copy(source, target);
                    if (LCTX.isDebugEnabled()) {
                        LCTX.d("File copying completed: " + target.getName());
                    }
                } catch (final IOException ex) {
                    LCTX.e("File copying failed: " + ex.getMessage());
                }
            }
        }
    }
}
