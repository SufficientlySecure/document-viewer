package org.ebookdroid.common.cache;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import org.emdev.common.filesystem.FilePrefixFilter;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.StringUtils;

public class CacheManager extends org.emdev.common.cache.CacheManager {

    private static final Map<String, SoftReference<ThumbnailFile>> thumbmails = new HashMap<String, SoftReference<ThumbnailFile>>();

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

    public static void clear() {
        thumbmails.clear();
        org.emdev.common.cache.CacheManager.clear();
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
}
