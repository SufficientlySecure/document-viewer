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

public class ThumbnailFile extends File {

    private static final long serialVersionUID = 4540533658351961301L;

    private static Bitmap defaultImage;

    private Bitmap ref;

    ThumbnailFile(final File dir, final String name) {
        super(dir, name);
        ref = null;
    }

    public Bitmap getImage() {
        if (ref == null) {
            try {
                ref = load(false);
            } catch (final OutOfMemoryError ex) {
            }
        }
        return ref != null && !ref.isRecycled() ? ref : null;
    }


    public Bitmap getImageAsync(final ImageLoadingListener l) {
        if (ref != null && !ref.isRecycled()) {
            return ref;
        }

        new AsyncTask<Void, Void, Bitmap>() {

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
                ref = result;
                l.onImageLoaded(ref);
            }
        }.execute();

        return null;
    }

    public Bitmap getRawImage() {
        if (ref == null) {
            try {
                ref = load(true);
            } catch (final OutOfMemoryError ex) {
            }
        }
        return ref != null && !ref.isRecycled() ? ref : null;
    }

    public void setImage(final Bitmap image) {
        if (image != null) {
            ref = paint(image);
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

}
