package org.ebookdroid.ui.opds.adapters;

import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.cache.ThumbnailFile;
import org.ebookdroid.opds.model.Book;
import org.ebookdroid.opds.model.Feed;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

import java.io.File;
import java.io.FileInputStream;
import java.util.ConcurrentModificationException;

import org.emdev.ui.tasks.AsyncTask;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.concurrent.Flag;

final class LoadThumbnailTask extends AsyncTask<Feed, Book, String> {

    private final OPDSAdapter adapter;

    private final Flag stopped = new Flag();

    LoadThumbnailTask(OPDSAdapter adapter) {
        this.adapter = adapter;
    }

    public void stop() {
        stopped.set();
    }

    @Override
    protected String doInBackground(final Feed... params) {
        if (LengthUtils.isEmpty(params)) {
            return null;
        }
        for (final Feed feed : params) {
            if (feed == null) {
                continue;
            }
            while (true) {
                try {
                    for (final Book book : feed.books) {
                        if (stopped.get() || adapter.currentFeed != feed) {
                            return null;
                        }
                        loadBookThumbnail(book);
                        publishProgress(book);
                    }
                    break;
                } catch (ConcurrentModificationException ex) {
                    if (stopped.get() || adapter.currentFeed != feed) {
                        return null;
                    }
                    // else repeat scanning
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(final String result) {
        adapter.notifyDataSetInvalidated();
    }

    @Override
    protected void onProgressUpdate(final Book... books) {
        boolean inCurrent = false;
        for (final Book book : books) {
            inCurrent |= book.parent == adapter.currentFeed;
        }
        if (inCurrent) {
            adapter.notifyDataSetInvalidated();
        }
    }

    protected void loadBookThumbnail(final Book book) {
        if (book.thumbnail == null) {
            return;
        }
        final ThumbnailFile thumbnailFile = CacheManager.getThumbnailFile(book.id);
        if (thumbnailFile.exists()) {
            return;
        }

        try {
            final File file = adapter.client.loadFile(book.parent, book.thumbnail);
            if (file == null) {
                return;
            }

            final Options opts = new Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            opts.inJustDecodeBounds = true;

            BitmapFactory.decodeStream(new FileInputStream(file), null, opts);

            opts.inSampleSize = getScale(opts, 200, 200);
            opts.inJustDecodeBounds = false;

            final Bitmap image = BitmapFactory.decodeStream(new FileInputStream(file), null, opts);
            if (image != null) {
                thumbnailFile.setImage(image);
                image.recycle();
            }
        } catch (final Throwable ex) {
            ex.printStackTrace();
        }
    }

    protected int getScale(final Options opts, final float requiredWidth, final float requiredHeight) {
        int scale = 1;
        int widthTmp = opts.outWidth;
        int heightTmp = opts.outHeight;
        while (true) {
            if (widthTmp / 2 < requiredWidth || heightTmp / 2 < requiredHeight) {
                break;
            }
            widthTmp /= 2;
            heightTmp /= 2;

            scale *= 2;
        }
        return scale;
    }

}
