package org.ebookdroid.core.crop;

import org.ebookdroid.common.bitmaps.IBitmapRef;
import org.ebookdroid.common.bitmaps.RawBitmap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

public class PageCropper {

    private static final int COLUMN_HALF_HEIGHT = 15;

    public static final int BMP_SIZE = 400;

    private final static int V_LINE_SIZE = 5;

    private final static int H_LINE_SIZE = 5;

    private static final int LINE_MARGIN = 20;

    private static final double WHITE_THRESHOLD = 0.005;

    private static RawBitmap VLINE = new RawBitmap(V_LINE_SIZE, BMP_SIZE - 2 * LINE_MARGIN, false);
    private static RawBitmap HLINE = new RawBitmap(BMP_SIZE - 2 * LINE_MARGIN, H_LINE_SIZE, false);

    private static RawBitmap CENTER = new RawBitmap(BMP_SIZE / 5, BMP_SIZE / 5, false);

    private static RawBitmap WHOLE = new RawBitmap(BMP_SIZE, BMP_SIZE, false);

    private static Bitmap BITMAP = Bitmap.createBitmap(BMP_SIZE, BMP_SIZE, Bitmap.Config.RGB_565);

    private static final Rect RECT = new Rect(0, 0, BMP_SIZE, BMP_SIZE);

    private static final int COLUMN_WIDTH = 5;

    private PageCropper() {
    }

    public static synchronized RectF getCropBounds(final IBitmapRef bitmap, final Rect bitmapBounds,
            final RectF pageSliceBounds) {
        Canvas c = new Canvas(BITMAP);
        bitmap.draw(c, bitmapBounds, RECT, null);

        final float avgLum = calculateAvgLum();

        final float left = getLeftBound(avgLum);
        final float right = getRightBound(avgLum);
        final float top = getTopBound(avgLum);
        final float bottom = getBottomBound(avgLum);

        return new RectF(left * pageSliceBounds.width() + pageSliceBounds.left, top * pageSliceBounds.height()
                + pageSliceBounds.top, right * pageSliceBounds.width() + pageSliceBounds.left, bottom
                * pageSliceBounds.height() + pageSliceBounds.top);
    }

    public static synchronized RectF getColumn(IBitmapRef bitmap, Rect bitmapBounds, float x, float y) {
        Canvas c = new Canvas(BITMAP);
        bitmap.draw(c, bitmapBounds, RECT, null);
        final float avgLum = calculateAvgLum();
        final float left = getLeftBound(avgLum, x, y);
        final float right = getRightBound(avgLum, x, y);

        return new RectF(left, 0, right, 1);
    }

    private static float getLeftBound(float avgLum, float x, float y) {
        boolean blackFound = false;
        int pointX = (int) (BMP_SIZE * x);
        int pointY = (int) (BMP_SIZE * y);
        int top = Math.max(0, pointY - COLUMN_HALF_HEIGHT);
        int bottom = Math.min(BMP_SIZE - 1, pointY + COLUMN_HALF_HEIGHT);

        RawBitmap column = new RawBitmap(COLUMN_WIDTH, bottom - top, false);
        for (int left = pointX; left >= 0; left -= COLUMN_WIDTH) {
            column.retrieve(BITMAP, left, top);
            if (isRectWhite(column, avgLum)) {
                if (blackFound) {
                    return ((float) left / BMP_SIZE);
                }
            } else {
                blackFound = true;
            }
        }
        return 0;
    }

    private static float getRightBound(float avgLum, float x, float y) {
        boolean blackFound = false;
        int pointX = (int) (BMP_SIZE * x);
        int pointY = (int) (BMP_SIZE * y);
        int top = Math.max(0, pointY - COLUMN_HALF_HEIGHT);
        int bottom = Math.min(BMP_SIZE - 1, pointY + COLUMN_HALF_HEIGHT);

        RawBitmap column = new RawBitmap(COLUMN_WIDTH, bottom - top, false);
        for (int left = pointX; left < BMP_SIZE - COLUMN_WIDTH; left += COLUMN_WIDTH) {
            column.retrieve(BITMAP, left, top);
            if (isRectWhite(column, avgLum)) {
                if (blackFound) {
                    return ((float) (left + COLUMN_WIDTH) / BMP_SIZE);
                }
            } else {
                blackFound = true;
            }
        }
        return 1;
    }

    private static float getLeftBound(final float avgLum) {
        final int w = BITMAP.getWidth() / 3;
        int whiteCount = 0;
        int x = 0;

        for (x = RECT.left; x < RECT.left + w; x += V_LINE_SIZE) {
            VLINE.retrieve(BITMAP, x, RECT.top + LINE_MARGIN, V_LINE_SIZE, VLINE.getHeight());
            final boolean white = isRectWhite(VLINE, avgLum);
            if (white) {
                whiteCount++;
            } else {
                if (whiteCount >= 1) {
                    return (float) (Math.max(RECT.left, x - V_LINE_SIZE) - RECT.left) / RECT.width();
                }
                whiteCount = 0;
            }
        }
        return whiteCount > 0 ? (float) (Math.max(RECT.left, x - V_LINE_SIZE) - RECT.left) / RECT.width() : 0;
    }

    private static float getTopBound(final float avgLum) {
        final int h = BITMAP.getHeight() / 3;
        int whiteCount = 0;
        int y = 0;

        for (y = RECT.top; y < RECT.top + h; y += H_LINE_SIZE) {
            HLINE.retrieve(BITMAP, RECT.left + LINE_MARGIN, y, HLINE.getWidth(), H_LINE_SIZE);
            final boolean white = isRectWhite(HLINE, avgLum);
            if (white) {
                whiteCount++;
            } else {
                if (whiteCount >= 1) {
                    return (float) (Math.max(RECT.top, y - H_LINE_SIZE) - RECT.top) / RECT.height();
                }
                whiteCount = 0;
            }
        }
        return whiteCount > 0 ? (float) (Math.max(RECT.top, y - H_LINE_SIZE) - RECT.top) / RECT.height() : 0;
    }

    private static float getRightBound(final float avgLum) {
        final int w = BITMAP.getWidth() / 3;
        int whiteCount = 0;
        int x = 0;

        for (x = RECT.right - V_LINE_SIZE; x > RECT.right - w; x -= V_LINE_SIZE) {
            VLINE.retrieve(BITMAP, x, RECT.top + LINE_MARGIN, V_LINE_SIZE, VLINE.getHeight());
            final boolean white = isRectWhite(VLINE, avgLum);
            if (white) {
                whiteCount++;
            } else {
                if (whiteCount >= 1) {
                    return (float) (Math.min(RECT.right, x + 2 * V_LINE_SIZE) - RECT.left) / RECT.width();
                }
                whiteCount = 0;
            }
        }
        return whiteCount > 0 ? (float) (Math.min(RECT.right, x + 2 * V_LINE_SIZE) - RECT.left) / RECT.width() : 1;
    }

    private static float getBottomBound(final float avgLum) {
        final int h = BITMAP.getHeight() / 3;
        int whiteCount = 0;
        int y = 0;
        for (y = RECT.bottom - H_LINE_SIZE; y > RECT.bottom - h; y -= H_LINE_SIZE) {
            HLINE.retrieve(BITMAP, RECT.left + LINE_MARGIN, y, HLINE.getWidth(), H_LINE_SIZE);
            final boolean white = isRectWhite(HLINE, avgLum);
            if (white) {
                whiteCount++;
            } else {
                if (whiteCount >= 1) {
                    return (float) (Math.min(RECT.bottom, y + 2 * H_LINE_SIZE) - RECT.top) / RECT.height();
                }
                whiteCount = 0;
            }
        }
        return whiteCount > 0 ? (float) (Math.min(RECT.bottom, y + 2 * H_LINE_SIZE) - RECT.top) / RECT.height() : 1;
    }

    private static boolean isRectWhite(RawBitmap rb, final float avgLum) {
        int count = 0;

        final int[] pixels = rb.getPixels();
        for (final int c : pixels) {
            final float lum = getLum(c);
            if ((lum < avgLum) && ((avgLum - lum) * 10 > avgLum)) {
                count++;
            }
        }
        return ((float) count / pixels.length) < WHITE_THRESHOLD;
    }

    private static float calculateAvgLum() {
        WHOLE.retrieve(BITMAP, 0, 0, WHOLE.getWidth(), WHOLE.getHeight());
        return WHOLE.getAvgLum();
    }

    private static float getLum(final int c) {
        final int r = (c & 0xFF0000) >> 16;
        final int g = (c & 0xFF00) >> 8;
        final int b = c & 0xFF;

        // return (77 * r + 150 * g + 29 * b) >> 8;
        final int min = Math.min(r, Math.min(g, b));
        final int max = Math.max(r, Math.max(g, b));
        return (min + max) / 2;
    }
}
