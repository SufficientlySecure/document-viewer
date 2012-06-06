package org.ebookdroid.common.cache;

import org.ebookdroid.common.log.LogContext;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import org.emdev.utils.FileUtils;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.StringUtils;
import org.emdev.utils.filesystem.FilePrefixFilter;

public class CacheManager {

    public static final LogContext LCTX = LogContext.ROOT.lctx("CacheManager");

    private static Context s_context;

    private static final Map<String, SoftReference<ThumbnailFile>> thumbmails = new HashMap<String, SoftReference<ThumbnailFile>>();

    public static void init(final Context context) {
        s_context = context;
    }

    public static ThumbnailFile getThumbnailFile(final String path) {
        final String md5 = StringUtils.md5(path);

        final SoftReference<ThumbnailFile> ref = thumbmails.get(md5);
        ThumbnailFile file = ref != null ? ref.get() : null;
        if (file == null) {
            final File cacheDir = s_context.getFilesDir();
            file = new ThumbnailFile(cacheDir, md5 + ".thumbnail");
            thumbmails.put(md5, new SoftReference<ThumbnailFile>(file));
        }

        return file;
    }

    public static PageCacheFile getPageFile(final String path) {
        final String md5 = StringUtils.md5(path);
        final File cacheDir = s_context.getFilesDir();
        return new PageCacheFile(cacheDir, md5 + ".cache");
    }

    public static File createTempFile(final InputStream source, final String suffix) throws IOException {
        final File cacheDir = s_context.getFilesDir();
        final File tempfile = File.createTempFile("ebookdroid", suffix, cacheDir);
        tempfile.deleteOnExit();

        FileUtils.copy(source, new FileOutputStream(tempfile));

        return tempfile;
    }

    public static File createTempFile(final Uri uri) throws IOException {
        final File cacheDir = s_context.getFilesDir();
        final File tempfile = File.createTempFile("ebookdroid", "content", cacheDir);
        tempfile.deleteOnExit();

        final InputStream source = s_context.getContentResolver().openInputStream(uri);
        FileUtils.copy(source, new FileOutputStream(tempfile));

        return tempfile;
    }

    public static void clear() {
        thumbmails.clear();

        final File cacheDir = s_context.getFilesDir();
        final String[] files = cacheDir.list();
        if (LengthUtils.isNotEmpty(files)) {
            for (final String file : files) {
                new File(cacheDir, file).delete();
            }
        }
    }

    public static void clear(final String path) {
        final String md5 = StringUtils.md5(path);
        thumbmails.remove(md5);

        final File cacheDir = s_context.getFilesDir();
        final String[] files = cacheDir.list(new FilePrefixFilter(md5 + "."));
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
