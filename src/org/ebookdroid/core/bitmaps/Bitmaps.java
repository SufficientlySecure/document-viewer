package org.ebookdroid.core.bitmaps;

import org.ebookdroid.core.PagePaint;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.core.log.LogContext;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region.Op;

import java.util.Collections;
import java.util.List;

public class Bitmaps {

    private static final LogContext LCTX = BitmapManager.LCTX;

    private static final int SIZE = 128;

    private static final Config DEF_BITMAP_TYPE = Bitmap.Config.RGB_565;

    private static boolean useDefaultBitmapType = true;

    public final Bitmap.Config config;
    public Rect bounds;
    public int columns;
    public int rows;

    private BitmapRef[] bitmaps;

    public Bitmaps(final String nodeId, final BitmapRef orig, final Rect bitmapBounds, final boolean invert) {
        final Bitmap origBitmap = orig.getBitmap();

        this.bounds = bitmapBounds;
        this.columns = (int) Math.ceil(bounds.width() / (float) SIZE);
        this.rows = (int) Math.ceil(bounds.height() / (float) SIZE);
        this.config = useDefaultBitmapType ? DEF_BITMAP_TYPE : origBitmap.getConfig();
        this.bitmaps = new BitmapRef[columns * rows];

        int top = 0;

        final RawBitmap rb = new RawBitmap(SIZE, SIZE, origBitmap.hasAlpha());

        for (int row = 0; row < rows; row++, top += SIZE) {
            int left = 0;
            for (int col = 0; col < columns; col++, left += SIZE) {
                final String name = nodeId + ":[" + row + ", " + col + "]";
                final BitmapRef b = BitmapManager.getBitmap(name, SIZE, SIZE, config);
                final Bitmap bmp = b.getBitmap();

                if (row == rows - 1 || col == columns - 1) {
                    final int right = Math.min(left + SIZE, bounds.width());
                    final int bottom = Math.min(top + SIZE, bounds.height());
                    rb.retrieve(origBitmap, left, top, right - left, bottom - top);
                } else {
                    rb.retrieve(origBitmap, left, top, SIZE, SIZE);
                }
                if (invert) {
                    rb.invert();
                }
                rb.toBitmap(bmp);

                final int index = row * columns + col;
                bitmaps[index] = b;
            }
        }
    }

    public synchronized boolean reuse(final String nodeId, final BitmapRef orig, final Rect bitmapBounds,
            final boolean invert) {
        final Bitmap origBitmap = orig.getBitmap();
        final Config cfg = useDefaultBitmapType ? DEF_BITMAP_TYPE : origBitmap.getConfig();
        if (cfg != this.config) {
            return false;
        }

        final BitmapRef[] oldBitmaps = this.bitmaps;

        this.bounds = bitmapBounds;
        this.columns = (int) Math.ceil(bitmapBounds.width() / (float) SIZE);
        this.rows = (int) Math.ceil(bitmapBounds.height() / (float) SIZE);
        this.bitmaps = new BitmapRef[columns * rows];

        final int newsize = this.columns * this.rows;

        int i = 0;
        for (; i < newsize; i++) {
            this.bitmaps[i] = i < oldBitmaps.length ? oldBitmaps[i] : null;
            if (this.bitmaps[i] == null || this.bitmaps[i].getBitmap() == null) {
                BitmapManager.release(this.bitmaps[i]);
                this.bitmaps[i] = BitmapManager.getBitmap(nodeId + ":reuse:" + i, SIZE, SIZE, config);
            } else {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("Reuse  bitmap: " + this.bitmaps[i]);
                }
            }
            this.bitmaps[i].getBitmap().eraseColor(Color.CYAN);
        }
        for (; i < oldBitmaps.length; i++) {
            BitmapManager.release(oldBitmaps[i]);
        }

        int top = 0;

        final RawBitmap rb = new RawBitmap(SIZE, SIZE, origBitmap.hasAlpha());

        for (int row = 0; row < rows; row++, top += SIZE) {
            int left = 0;
            for (int col = 0; col < columns; col++, left += SIZE) {
                final int index = row * columns + col;
                final BitmapRef b = bitmaps[index];
                final Bitmap bmp = b.getBitmap();

                if (row == rows - 1 || col == columns - 1) {
                    final int right = Math.min(left + SIZE, bounds.width());
                    final int bottom = Math.min(top + SIZE, bounds.height());
                    rb.retrieve(origBitmap, left, top, right - left, bottom - top);
                } else {
                    rb.retrieve(origBitmap, left, top, SIZE, SIZE);
                }
                if (invert) {
                    rb.invert();
                }
                rb.toBitmap(bmp);
            }
        }

        return true;
    }

    public synchronized Bitmap[] getBitmaps() {
        if (bitmaps == null) {
            return null;
        }
        final Bitmap[] res = new Bitmap[bitmaps.length];
        for (int i = 0; i < bitmaps.length; i++) {
            res[i] = bitmaps[i] != null ? bitmaps[i].getBitmap() : null;
            if (res[i] == null || res[i].isRecycled()) {
                recycle(null);
                return null;
            }
        }
        return res;
    }

    public synchronized void clearDirectRef() {
        if (bitmaps != null) {
            for (final BitmapRef b : bitmaps) {
                b.clearDirectRef();
            }
        }
    }

    public synchronized void recycle(final List<BitmapRef> bitmapsToRecycle) {
        if (bitmaps != null) {
            if (bitmapsToRecycle != null) {
                Collections.addAll(bitmapsToRecycle, bitmaps);
            } else {
                BitmapManager.release(bitmaps);
            }
            bitmaps = null;
        }
    }

    public synchronized void draw(final ViewState viewState, final Canvas canvas, final PagePaint paint, final RectF tr) {
        final Bitmap[] bitmap = getBitmaps();
        if (bitmap != null) {
            final Rect orig = canvas.getClipBounds();
            canvas.clipRect(tr, Op.INTERSECT);

            final float scaleX = tr.width() / bounds.width();
            final float scaleY = tr.height() / bounds.height();

            int top = 0;
            for (int row = 0; row < rows; row++, top += SIZE) {
                int left = 0;
                for (int col = 0; col < columns; col++, left += SIZE) {
                    final Matrix m = new Matrix();
                    m.postTranslate(left, top);
                    m.postScale(scaleX, scaleY);
                    m.postTranslate(tr.left, tr.top);

                    final int index = row * columns + col;
                    if (bitmap[index] != null && !bitmap[index].isRecycled()) {
                        canvas.drawBitmap(bitmap[index], m, paint.bitmapPaint);
                    }
                }
            }
            canvas.clipRect(orig, Op.REPLACE);
        }
    }
}
