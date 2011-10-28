package org.ebookdroid.core.cache;

import org.ebookdroid.utils.StringUtils;

import android.content.Context;

import java.io.File;

public class CacheManager {

    private static Context s_context;

    public static void init(Context context) {
        s_context = context;
    }

    public static File getThumbnailFile(final String path) {
        final String md5 = StringUtils.md5(path);
        final File cacheDir = s_context.getFilesDir();
        return new File(cacheDir, md5 + ".thumbnail");
    }
    
    public static File getPageFile(String path) {
        final String md5 = StringUtils.md5(path);
        final File cacheDir = s_context.getFilesDir();
        return new File(cacheDir, md5 + ".cache");
    }
    
    public static void clear() {
        final File cacheDir = s_context.getFilesDir();
        String[] files = cacheDir.list();
        for (String file : files) {
            new File(cacheDir, file).delete();
        }
    }
}
