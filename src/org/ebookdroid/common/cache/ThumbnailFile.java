package org.ebookdroid.common.cache;

import org.ebookdroid.R;
import org.ebookdroid.common.bitmaps.BitmapManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class ThumbnailFile extends File {

    private static final long serialVersionUID = 4540533658351961301L;

    private static Bitmap defaultImage;

    private final AtomicReference<Bitmap> ref = new AtomicReference<Bitmap>();

    private final AtomicReference<LoadingTask> task = new AtomicReference<ThumbnailFile.LoadingTask>();

    ThumbnailFile(final File dir, final String name) {
        super(dir, name);
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

    public Bitmap getImageAsync(final ImageLoadingListener l) {
        final Bitmap bitmap = ref.get();
        if (bitmap != null && !bitmap.isRecycled()) {
            return bitmap;
        }

        if (task.compareAndSet(null, new LoadingTask(l))) {
            task.get().execute();
        }

        return null;
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

        final Bitmap cornerBmp = BitmapManager.getResource(R.drawable.bt_corner);
        final Bitmap leftBmp = BitmapManager.getResource(R.drawable.bt_left);
        final Bitmap topBmp = BitmapManager.getResource(R.drawable.bt_top);

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

        private final ImageLoadingListener listener;

        private LoadingTask(final ImageLoadingListener l) {
            listener = l;
        }

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
            ref.set(result);
            task.compareAndSet(this, null);
            listener.onImageLoaded(result);
        }
    }
}
