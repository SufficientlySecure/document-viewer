package org.ebookdroid.cbdroid.codec;

import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.utils.archives.ArchiveEntry;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

public class CbxPage<ArchiveEntryType extends ArchiveEntry> implements CodecPage {

    private static final String LCTX = "CbxPage";

    private final ArchiveEntryType entry;

    private CodecPageInfo pageInfo;

    public CbxPage(final ArchiveEntryType entry) {
        this.entry = entry;
    }

    private Bitmap decode(final boolean onlyBounds) {
        if (entry == null) {
            return null;
        }

        Log.d(LCTX, "Starting " + (onlyBounds ? " partial" : "full") + " decompressing: " + entry.getName());
        try {
            final InputStream is = entry.open();
            try {
                final Options opts = new Options();
                opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                opts.inJustDecodeBounds = onlyBounds;
                final Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);
                pageInfo = new CodecPageInfo();
                if (onlyBounds) {
                    pageInfo.setHeight(opts.outHeight);
                    pageInfo.setWidth(opts.outWidth);
                } else {
                    pageInfo.setHeight(bitmap.getHeight());
                    pageInfo.setWidth(bitmap.getWidth());
                }
                return bitmap;
            } finally {
                try {
                    is.close();
                } catch (final IOException ex) {
                }
                Log.d(LCTX, "Finishing" + (onlyBounds ? " partial" : "full") + " decompressing: " + entry.getName());
            }
        } catch (final Throwable e) {
            Log.d(LCTX, "Can not decompress page: " + e.getMessage());
            return null;
        }
    }

    @Override
    public int getHeight() {
        return getPageInfo().getHeight();
    }

    @Override
    public int getWidth() {
        return getPageInfo().getWidth();
    }

    @Override
    public void recycle() {
    }

    @Override
    public Bitmap renderBitmap(final int width, final int height, final RectF pageSliceBounds) {
        final Bitmap bitmap = decode(false);
        if (bitmap == null) {
            return null;
        }
        try {
            final Matrix matrix = new Matrix();
            matrix.postScale((float) width / getWidth(), (float) height / getHeight());
            matrix.postTranslate(-pageSliceBounds.left * width, -pageSliceBounds.top * height);
            matrix.postScale(1 / pageSliceBounds.width(), 1 / pageSliceBounds.height());

            final Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            final Canvas c = new Canvas(bmp);
            final Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            paint.setDither(true);
            c.drawBitmap(bitmap, matrix, null);

            return bmp;
        } finally {
            bitmap.recycle();
        }
    }

    public CodecPageInfo getPageInfo() {
        if (pageInfo == null) {
            decode(true);
        }
        return pageInfo;
    }
}
