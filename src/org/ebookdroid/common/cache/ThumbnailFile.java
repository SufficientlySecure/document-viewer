package org.ebookdroid.common.cache;

import org.ebookdroid.R;
import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.cache.CacheManager.ICacheListener;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.emdev.ui.tasks.AsyncTask;
import org.emdev.ui.tasks.AsyncTaskExecutor;

public class ThumbnailFile extends File {

    private static final long serialVersionUID = 4540533658351961301L;

    private static final AsyncTaskExecutor executor = new AsyncTaskExecutor(256, 1, 5, 1, "ThumbnailLoader");

    public final String book;

    private static Bitmap defaultImage;

    private final AtomicReference<Bitmap> ref = new AtomicReference<Bitmap>();

    private ImageLoadingListener listener;

    ThumbnailFile(final String book, final File dir, final String name) {
        super(dir, name);
        this.book = book;
    }

    public Bitmap getImage() {
        Bitmap bitmap = ref.get();
        if (bitmap == null || bitmap.isRecycled()) {
            try {
                bitmap = load(false);
                ref.set(bitmap);
            } catch (final OutOfMemoryError ex) {
            }
        }
        return bitmap;
    }

    public synchronized void loadImageAsync(final Bitmap defImage, final ImageLoadingListener l) {
        final Bitmap bitmap = ref.get();
        if (bitmap != null && !bitmap.isRecycled()) {
            l.onImageLoaded(bitmap);
            return;
        }

        l.onImageLoaded(defImage);
        listener = l;
        executor.execute(new LoadingTask());
    }

    protected synchronized void onImageLoaded(final Bitmap result) {
        ref.set(result);
        if (listener != null) {
            listener.onImageLoaded(result);
        }
    }

    public Bitmap getRawImage() {
        Bitmap bitmap = ref.get();
        if (bitmap == null || bitmap.isRecycled()) {
            try {
                bitmap = load(true);
                ref.set(bitmap);
            } catch (final OutOfMemoryError ex) {
            }
        }
        return bitmap;
    }

    public void setImage(final Bitmap image) {
        if (image != null) {
            ref.set(paint(image));
            store(image);
        } else {
            this.delete();
        }
        final ICacheListener l = CacheManager.listeners.getListener();
        l.onThumbnailChanged(this);
    }

    private Bitmap load(final boolean raw) {
        if (this.exists()) {
            final Bitmap stored = BitmapFactory.decodeFile(this.getPath());
            if (stored != null) {
                return raw ? stored : paint(stored);
            }
        }
        return getDefaultThumbnail();
    }

    private void store(final Bitmap image) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(this);
            image.compress(Bitmap.CompressFormat.JPEG, 50, out);
        } catch (final IOException e) {
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (final IOException ex) {
                }
            }
        }
    }

    private static Bitmap paint(final Bitmap image) {
        final int left = 15;
        final int top = 10;
        final int width = image.getWidth() + left;
        final int height = image.getHeight() + top;

        final Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        if (bmp == null) {
            return null;
        }

        bmp.eraseColor(Color.TRANSPARENT);

        final Canvas c = new Canvas(bmp);

        final Bitmap cornerBmp = BitmapManager.getResource(R.drawable.components_thumbnail_corner);
        final Bitmap leftBmp = BitmapManager.getResource(R.drawable.components_thumbnail_left);
        final Bitmap topBmp = BitmapManager.getResource(R.drawable.components_thumbnail_top);

        c.drawBitmap(cornerBmp, null, new Rect(0, 0, left, top), null);
        c.drawBitmap(topBmp, null, new Rect(left, 0, width, top), null);
        c.drawBitmap(leftBmp, null, new Rect(0, top, left, height), null);
        c.drawBitmap(image, null, new Rect(left, top, width, height), null);

        return bmp;
    }

    private static Bitmap getDefaultThumbnail() {
        if (defaultImage == null) {
            final Bitmap empty = Bitmap.createBitmap(160, 200, Bitmap.Config.ARGB_8888);
            if (empty != null) {
                empty.eraseColor(Color.WHITE);
                defaultImage = paint(empty);
            }
        }
        return defaultImage;
    }

    public static interface ImageLoadingListener {

        void onImageLoaded(Bitmap image);
    }

    private final class LoadingTask extends AsyncTask<Void, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(final Void... params) {
            try {
                return load(false);
            } catch (final OutOfMemoryError ex) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Bitmap result) {
            onImageLoaded(result);
        }
    }
}
