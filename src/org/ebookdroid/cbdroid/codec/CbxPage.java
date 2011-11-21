package org.ebookdroid.cbdroid.codec;

import org.ebookdroid.core.bitmaps.BitmapManager;
import org.ebookdroid.core.bitmaps.BitmapRef;
import org.ebookdroid.core.bitmaps.RawBitmap;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.utils.archives.ArchiveEntry;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CbxPage<ArchiveEntryType extends ArchiveEntry> implements CodecPage {

    private final ArchiveEntryType entry;

    private CodecPageInfo pageInfo;

    public CbxPage(final ArchiveEntryType entry) {
        this.entry = entry;
    }

    private Bitmap decode(final boolean onlyBounds) {
        return decode(onlyBounds, 1);
    }

    private Bitmap decode(final boolean onlyBounds, final int scale) {
        if (entry == null) {
            return null;
        }

        if (CbxDocument.LCTX.isDebugEnabled()) {
            CbxDocument.LCTX.d("Starting " + (onlyBounds ? " partial" : "full") + " decompressing: " + entry.getName());
        }
        try {
            final InputStream is = entry.open();
            try {
                final Options opts = new Options();
                opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                opts.inJustDecodeBounds = onlyBounds;
                opts.inSampleSize = scale;

                final Bitmap bitmap = BitmapFactory.decodeStream(new BufferedInputStream(is), null, opts);
                pageInfo = new CodecPageInfo();
                if (onlyBounds) {
                    pageInfo.height = (opts.outHeight * scale);
                    pageInfo.width = (opts.outWidth * scale);
                } else {
                    pageInfo.height = (bitmap.getHeight() * scale);
                    pageInfo.width = (bitmap.getWidth() * scale);
                }
                return bitmap;
            } finally {
                try {
                    is.close();
                } catch (final IOException ex) {
                }
                if (CbxDocument.LCTX.isDebugEnabled()) {
                    CbxDocument.LCTX.d("Finishing" + (onlyBounds ? " partial" : "full") + " decompressing: "
                            + entry.getName());
                }
            }
        } catch (final Throwable e) {
            if (CbxDocument.LCTX.isDebugEnabled()) {
                CbxDocument.LCTX.d("Can not decompress page: " + e.getMessage());
            }
            return null;
        }
    }

    @Override
    public int getHeight() {
        return getPageInfo().height;
    }

    @Override
    public int getWidth() {
        return getPageInfo().width;
    }

    @Override
    public void recycle() {
        if (storedBitmap != null) {
            storedBitmap.recycle();
            storedBitmap = null;
        }
    }

    @Override
    public boolean isRecycled() {
        return false;
    }

    private Bitmap storedBitmap;
    private int storedScale = Integer.MAX_VALUE;

    @Override
    public BitmapRef renderBitmap(final int width, final int height, final RectF pageSliceBounds) {
        if (getPageInfo() == null) {
            return null;
        }

        CbxDocument.LCTX.d("Rendering bitmap: [" + width + ", " + height + "] :" + pageSliceBounds);

        float requiredWidth = (float) width / pageSliceBounds.width();
        float requiredHeight = (float) height / pageSliceBounds.height();

        int scale = 1;
        int widthTmp = getWidth();
        int heightTmp = getHeight();
        while (true) {
            if (widthTmp / 2 < requiredWidth || heightTmp / 2 < requiredHeight) {
                break;
            }
            widthTmp /= 2;
            heightTmp /= 2;

            scale *= 2;
        }

        CbxDocument.LCTX.d("storedScale=" + storedScale + ", scale=" + scale + ", " + (storedBitmap == null));
        if (storedBitmap == null || storedScale > scale) {
            if (storedBitmap != null && !storedBitmap.isRecycled()) {
                storedBitmap.recycle();
            }
            storedBitmap = decode(false, scale);
            storedScale = scale;
        }
        if (storedBitmap == null) {
            return null;
        }

        final BitmapRef bmp = BitmapManager.getBitmap(width, height, Bitmap.Config.RGB_565);

        final Canvas c = new Canvas(bmp.getBitmap());
        final Paint paint = new Paint();
        paint.setFilterBitmap(true);
        paint.setAntiAlias(true);
        paint.setDither(true);

        Rect srcRect = new Rect((int) (pageSliceBounds.left * storedBitmap.getWidth()),
                (int) (pageSliceBounds.top * storedBitmap.getHeight()),
                (int) (pageSliceBounds.right * storedBitmap.getWidth()),
                (int) (pageSliceBounds.bottom * storedBitmap.getHeight()));

        float scaleFactor = (float)width/(float)srcRect.width();
        CbxDocument.LCTX.d("Scale factor:"+scaleFactor);
        if (scaleFactor > 4.5) {
            CbxDocument.LCTX.d("Calling Native HQ4x");
            RawBitmap src = new RawBitmap(storedBitmap, srcRect);
            Bitmap scaled = src.scaleHq4x();
            c.drawBitmap(scaled, null, new Rect(0,0,width, height), paint);
            scaled.recycle();
        } else if (scaleFactor > 3.5) {
            CbxDocument.LCTX.d("Calling Native HQ3x");
            RawBitmap src = new RawBitmap(storedBitmap, srcRect);
            Bitmap scaled = src.scaleHq3x();
            c.drawBitmap(scaled, null, new Rect(0,0,width, height), paint);
            scaled.recycle();
        } else if (scaleFactor > 2.5) {
            CbxDocument.LCTX.d("Calling Native HQ2x");
            RawBitmap src = new RawBitmap(storedBitmap, srcRect);
            Bitmap scaled = src.scaleHq2x();
            c.drawBitmap(scaled, null, new Rect(0,0,width, height), paint);
            scaled.recycle();
        } else {
            c.drawBitmap(storedBitmap, srcRect, new Rect(0,0,width, height), paint);
        }
        return bmp;
    }

    public CodecPageInfo getPageInfo() {
        if (pageInfo == null) {
            decode(true);
        }
        return pageInfo;
    }

}
