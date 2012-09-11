package org.ebookdroid.droids.cbx.codec;

import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.IBitmapRef;
import org.ebookdroid.common.bitmaps.RawBitmap;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.core.codec.AbstractCodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.FloatMath;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.emdev.common.archives.ArchiveEntry;

public class CbxPage<ArchiveEntryType extends ArchiveEntry> extends AbstractCodecPage {

    private static final Paint PAINT = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    private final ArchiveEntryType entry;

    private CodecPageInfo pageInfo;

    private int storedScale = Integer.MAX_VALUE;

    private Bitmap storedBitmap;

    public CbxPage(final ArchiveEntryType entry) {
        this.entry = entry;
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

                final Bitmap bitmap = BitmapFactory.decodeStream(new BufferedInputStream(is, 64 * 1024), null, opts);
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

    @Override
    public IBitmapRef renderBitmap(final ViewState viewState, final int width, final int height,
            final RectF pageSliceBounds) {
        if (getPageInfo() == null) {
            return null;
        }

        CbxDocument.LCTX.d("Rendering bitmap: [" + width + ", " + height + "] :" + pageSliceBounds);

        final float requiredWidth = width / pageSliceBounds.width();
        final float requiredHeight = height / pageSliceBounds.height();

        int scale = getScale(requiredWidth, requiredHeight);

        if (CbxDocument.LCTX.isDebugEnabled()) {
            CbxDocument.LCTX.d("storedScale=" + storedScale + ", scale=" + scale + ", " + (storedBitmap == null));
        }
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

        final IBitmapRef bmp = BitmapManager.getBitmap("CBX page", width, height, Bitmap.Config.RGB_565);

        final Canvas c = bmp.getCanvas();

        final Rect srcRect = new Rect((int) (pageSliceBounds.left * storedBitmap.getWidth()),
                (int) (pageSliceBounds.top * storedBitmap.getHeight()), (int) FloatMath.ceil(pageSliceBounds.right
                        * storedBitmap.getWidth()), (int) FloatMath.ceil(pageSliceBounds.bottom
                        * storedBitmap.getHeight()));

        if (CbxDocument.LCTX.isDebugEnabled()) {
            CbxDocument.LCTX.d("source ratio=" + (srcRect.width() / (float) srcRect.height()) + ", target ratio="
                    + (width / (float) height));
        }

        final float scaleFactor = (float) width / (float) srcRect.width();

        if (CbxDocument.LCTX.isDebugEnabled()) {
            CbxDocument.LCTX.d("Scale factor:" + scaleFactor);
        }
        if (scaleFactor > 4.5) {
            if (CbxDocument.LCTX.isDebugEnabled()) {
                CbxDocument.LCTX.d("Calling Native HQ4x");
            }
            final RawBitmap src = new RawBitmap(storedBitmap, srcRect);
            final IBitmapRef scaled = src.scaleHq4x();
            scaled.draw(c, null, new Rect(0, 0, width, height), PAINT);
            BitmapManager.release(scaled);
        } else if (scaleFactor > 3.5) {
            if (CbxDocument.LCTX.isDebugEnabled()) {
                CbxDocument.LCTX.d("Calling Native HQ3x");
            }
            final RawBitmap src = new RawBitmap(storedBitmap, srcRect);
            final IBitmapRef scaled = src.scaleHq3x();
            scaled.draw(c, null, new Rect(0, 0, width, height), PAINT);
            BitmapManager.release(scaled);
        } else if (scaleFactor > 2.5) {
            if (CbxDocument.LCTX.isDebugEnabled()) {
                CbxDocument.LCTX.d("Calling Native HQ2x");
            }
            final RawBitmap src = new RawBitmap(storedBitmap, srcRect);
            final IBitmapRef scaled = src.scaleHq2x();
            scaled.draw(c, null, new Rect(0, 0, width, height), PAINT);
            BitmapManager.release(scaled);
        } else {
            c.drawBitmap(storedBitmap, srcRect, new Rect(0, 0, width, height), PAINT);
        }
        return bmp;
    }

    private int getScale(final float requiredWidth, final float requiredHeight) {
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
        return scale;
    }

    public CodecPageInfo getPageInfo() {
        if (pageInfo == null) {
            decode(true, 1);
        }
        return pageInfo;
    }

}
