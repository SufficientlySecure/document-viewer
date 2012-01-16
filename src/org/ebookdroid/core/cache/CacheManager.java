package org.ebookdroid.core.cache;

import org.ebookdroid.core.utils.FilePrefixFilter;
import org.ebookdroid.utils.StringUtils;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class CacheManager {

    private static Context s_context;

    public static void init(final Context context) {
        s_context = context;
    }

    public static File getThumbnailFile(final String path) {
        final String md5 = StringUtils.md5(path);
        final File cacheDir = s_context.getFilesDir();
        return new File(cacheDir, md5 + ".thumbnail");
    }

    public static File getPageFile(final String path) {
        final String md5 = StringUtils.md5(path);
        final File cacheDir = s_context.getFilesDir();
        return new File(cacheDir, md5 + ".cache");
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
        for (final String file : files) {
            new File(cacheDir, file).delete();
        }
    }

    public static void clear(final String path) {
        final File cacheDir = s_context.getFilesDir();
        final String[] files = cacheDir.list(new FilePrefixFilter(StringUtils.md5(path) + "."));
        for (final String file : files) {
            new File(cacheDir, file).delete();
        }
    }

}
