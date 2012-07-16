package org.emdev.utils.textmarkup.image;

import org.ebookdroid.common.cache.CacheManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class DiskImageData extends AbstractImageData implements Runnable {

    final AtomicReference<File> file = new AtomicReference<File>();
    final AtomicReference<MemoryImageData> cachedData;
    final Options imageSize;

    public DiskImageData(final MemoryImageData data) {
        cachedData = new AtomicReference<MemoryImageData>(data);
        imageSize = data.getImageSize();
        this.run();
    }

    @Override
    public Bitmap getBitmap() {
        final File f = file.get();
        if (f != null) {
            return BitmapFactory.decodeFile(f.getAbsolutePath());
        }
        final MemoryImageData data = cachedData.get();
        if (data != null) {
            return data.getBitmap();
        }

        return null;
    }

    @Override
    protected Options getImageSize() {
        return imageSize;
    }

    @Override
    public void run() {
        Thread.currentThread().setPriority(8);
        try {
            if (file.get() != null) {
                return;
            }
            final MemoryImageData data = cachedData.get();
            if (data == null) {
                return;
            }
            final File f = store(data);
            if (f != null) {
                file.set(f);
                cachedData.set(null);
                data.data = null;
                data.encoded = null;
            }
        } catch (final Throwable th) {
            th.printStackTrace();
        }
    }

    private static File store(final MemoryImageData data) {
        try {
            final byte[] d = data.getData();
            final File file = CacheManager.createTempFile(d, ".fb2.img");
            return file;
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
