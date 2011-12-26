package org.ebookdroid.core.bitmaps;

import org.ebookdroid.core.PagePaint;
import org.ebookdroid.core.ViewState;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.Arrays;

public class Bitmaps {

    private static final int SIZE = 128;

    public final Rect bounds;
    public final int columns;
    public final int rows;

    private BitmapRef[] bitmaps;

    public Bitmaps(final String nodeId, final BitmapRef orig, final Rect bitmapBounds) {
        this.bounds = bitmapBounds;

        columns = (int) Math.ceil(bounds.width() / (float) SIZE);
        rows = (int) Math.ceil(bounds.height() / (float) SIZE);

        final Bitmap origBitmap = orig.getBitmap();

        bitmaps = new BitmapRef[columns * rows];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                final Rect rect = getRect(row, col);
                final RawBitmap rb = new RawBitmap(origBitmap, rect);

                final String name = nodeId + ":" + row + ", " + col;
                final BitmapRef b = BitmapManager.getBitmap(name, 128, 128, origBitmap.getConfig());
                final Bitmap bmp = b.getBitmap();
                if (row == rows - 1 || col == columns - 1) {
                    bmp.eraseColor(Color.BLACK);
                }
                rb.toBitmap(bmp);

                final int index = row * columns + col;
                bitmaps[index] = b;
            }
        }
    }

    public Bitmaps(final String nodeId, final Bitmaps orig, final Bitmap[] days, final Paint paint) {
        this.bounds = orig.bounds;
        this.columns = orig.columns;
        this.rows = orig.rows;

        this.bitmaps = new BitmapRef[days.length];
        for (int i = 0; i < bitmaps.length; i++) {
            final String name = nodeId + ":night:" + i;
            final int w = days[i].getWidth();
            final int h = days[i].getHeight();
            bitmaps[i] = BitmapManager.getBitmap(name, w, h, Bitmap.Config.RGB_565);
            final Bitmap bmp = bitmaps[i].getBitmap();
            bmp.eraseColor(Color.WHITE);
            final Canvas c = new Canvas(bmp);
            c.drawRect(0, 0, w, h, paint);
            c.drawBitmap(days[i], 0, 0, paint);
        }
    }

    public synchronized Bitmap[] getBitmaps() {
        if (bitmaps == null) {
            return null;
        }
        final Bitmap[] res = new Bitmap[bitmaps.length];
        for (int i = 0; i < bitmaps.length; i++) {
            res[i] = bitmaps[i].getBitmap();
            if (res[i] == null || res[i].isRecycled()) {
                recycle();
                return null;
            }
        }
        return res;
    }

    public synchronized void clearDirectRef() {
        if (bitmaps != null) {
            for (BitmapRef b : bitmaps) {
                b.clearDirectRef();
            }
        }
    }

    public synchronized void recycle() {
        if (bitmaps != null) {
            BitmapManager.release(new ArrayList<BitmapRef>(Arrays.asList(bitmaps)));
            bitmaps = null;
        }
    }

    public synchronized void draw(final ViewState viewState, final Canvas canvas, final PagePaint paint, final RectF tr) {
        final Bitmap[] bitmap = getBitmaps();
        if (bitmap != null) {
            final Paint emp = new Paint();
            emp.setColor(Color.GRAY);
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < columns; col++) {
                    final RectF source = new RectF(getRect(row, col));
                    final Matrix m = new Matrix();
                    m.postScale(tr.width() / bounds.width(), tr.height() / bounds.height());
                    m.postTranslate(tr.left, tr.top);

                    final RectF target = new RectF();
                    m.mapRect(target, source);

                    final int index = row * columns + col;
                    if (bitmap[index] != null && !bitmap[index].isRecycled()) {
                        canvas.drawBitmap(bitmap[index], new Rect(0, 0, (int) source.width(), (int) source.height()),
                                target, paint.bitmapPaint);
                    } else {
                        canvas.drawRect(source, emp);
                    }
                }
            }
        }
    }

    public Rect getRect(final int row, final int col) {
        final int left = col * SIZE;
        final int top = row * SIZE;
        final int right = Math.min(left + SIZE, bounds.width());
        final int bottom = Math.min(top + SIZE, bounds.height());
        final Rect rect = new Rect(left, top, right, bottom);
        return rect;
    }
}
