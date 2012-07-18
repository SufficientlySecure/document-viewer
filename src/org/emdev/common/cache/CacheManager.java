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
import org.emdev.utils.FileUtils;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.StringUtils;

public class CacheManager {

    public static final LogContext LCTX = LogManager.root().lctx("CacheManager");

    protected static Context s_context;

    public static void init(final Context context) {
        s_context = context;
    }

    public static File createTempFile(final InputStream source, final String suffix) throws IOException {
        final File cacheDir = s_context.getFilesDir();
        final File tempfile = File.createTempFile("temp", suffix, cacheDir);
        tempfile.deleteOnExit();

        FileUtils.copy(source, new FileOutputStream(tempfile));

        return tempfile;
    }

    public static File createTempFile(final byte[] source, final String suffix) throws IOException {
        final File cacheDir = s_context.getFilesDir();
        final File tempfile = File.createTempFile("temp", suffix, cacheDir);
        tempfile.deleteOnExit();

        FileUtils.copy(new ByteArrayInputStream(source), new FileOutputStream(tempfile), source.length, null);

        return tempfile;
    }
    
    public static File createTempFile(final Uri uri) throws IOException {
        final File cacheDir = s_context.getFilesDir();
        final File tempfile = File.createTempFile("temp", "content", cacheDir);
        tempfile.deleteOnExit();

        final InputStream source = s_context.getContentResolver().openInputStream(uri);
        FileUtils.copy(source, new FileOutputStream(tempfile));

        return tempfile;
    }

    public static void clear() {
        final File cacheDir = s_context.getFilesDir();
        final String[] files = cacheDir.list();
        if (LengthUtils.isNotEmpty(files)) {
            for (final String file : files) {
                new File(cacheDir, file).delete();
            }
        }
    }

    public static void copy(final String sourcePath, final String targetPath, final boolean deleteSource) {
        final File cacheDir = s_context.getFilesDir();
        final String[] files = cacheDir.list(new FilePrefixFilter(StringUtils.md5(sourcePath) + "."));
        if (LengthUtils.isEmpty(files)) {
            return;
        }
        final String targetPrefix = StringUtils.md5(targetPath);
        for (final String file : files) {
            final File source = new File(cacheDir, file);
            final File target = new File(cacheDir, targetPrefix + FileUtils.getExtensionWithDot(source));
            if (deleteSource) {
                if (!source.renameTo(target)) {
                    try {
                        FileUtils.copy(source, target);
                        source.delete();
                        LCTX.e("Moving file completed: " + target.getName());
                    } catch (final IOException ex) {
                        LCTX.e("Moving file failed: " + ex.getMessage());
                    }
                }
            } else {
                try {
                    FileUtils.copy(source, target);
                    LCTX.e("Copying file completed: " + target.getName());
                } catch (final IOException ex) {
                    LCTX.e("Copying file failed: " + ex.getMessage());
                }
            }
        }
    }
}
