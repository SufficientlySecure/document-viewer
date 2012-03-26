package org.ebookdroid.common.cache;

import org.ebookdroid.common.log.LogContext;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Map;

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

    public static File createTempFile(final Uri uri) throws IOException {
        final File cacheDir = s_context.getFilesDir();
        File tempfile = null;
        tempfile = File.createTempFile("ebookdroid", "content", cacheDir);
        tempfile.deleteOnExit();

        ReadableByteChannel in = null;
        WritableByteChannel out = null;
        try {
            in = Channels.newChannel(s_context.getContentResolver().openInputStream(uri));
            out = Channels.newChannel(new FileOutputStream(tempfile));
            final ByteBuffer buf = ByteBuffer.allocate(1024 * 1024);
            while (in.read(buf) > 0) {
                buf.flip();
                out.write(buf);
                buf.flip();
            }
        } finally {
            try {
                out.close();
            } catch (final Exception ex) {
            }
            try {
                in.close();
            } catch (final Exception ex) {
            }
        }

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

    public static void clear(final String path) {
        final File cacheDir = s_context.getFilesDir();
        final String[] files = cacheDir.list(new FilePrefixFilter(StringUtils.md5(path) + "."));
        if (LengthUtils.isNotEmpty(files)) {
            for (final String file : files) {
                new File(cacheDir, file).delete();
            }
        }
    }

}
