package org.ebookdroid.core.crop;

import org.ebookdroid.core.bitmaps.BitmapRef;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

public class PageCropper {

    private final static int LINE_SIZE = 10;

    private PageCropper() {
    }

    public static RectF getCropBounds(BitmapRef bitmap, Rect bitmapBounds, RectF pageSliceBounds) {
        float avgLum = calculateAvgLum(bitmap, bitmapBounds);
        float left = getLeftBound(bitmap, bitmapBounds, avgLum);
        float right = getRightBound(bitmap, bitmapBounds, avgLum);
        float top = getTopBound(bitmap, bitmapBounds, avgLum);
        float bottom = getBottomBound(bitmap, bitmapBounds, avgLum);

        return new RectF(left * pageSliceBounds.width() + pageSliceBounds.left, top * pageSliceBounds.height()
                + pageSliceBounds.top, right * pageSliceBounds.width() + pageSliceBounds.left, bottom
                * pageSliceBounds.height() + pageSliceBounds.top);
    }

    private static float getLeftBound(BitmapRef bitmap, Rect bitmapBounds, float avgLum) {
        Bitmap bmp = bitmap.getBitmap();
        final int w = bmp.getWidth() / 3;
        int whiteCount = 0;
        int x = 0;
        for (x = bitmapBounds.left; x < bitmapBounds.left + w; x += LINE_SIZE) {
            boolean white = isRectWhite(bmp, x, bitmapBounds.top + 20, x + LINE_SIZE, bitmapBounds.bottom - 20, avgLum);
            if (white) {
                whiteCount++;
            } else {
                if (whiteCount >= 1) {
                    return (float) (Math.max(bitmapBounds.left, x - LINE_SIZE) - bitmapBounds.left) / bitmapBounds.width();
                }
                whiteCount = 0;
            }
        }
        return whiteCount > 0 ? (float) (Math.max(bitmapBounds.left, x - LINE_SIZE) - bitmapBounds.left) / bitmapBounds.width() : 0;
    }

    private static float getTopBound(BitmapRef bitmap, Rect bitmapBounds, float avgLum) {
        Bitmap bmp = bitmap.getBitmap();
        final int h = bmp.getHeight() / 3;
        int whiteCount = 0;
        int y = 0;
        for (y = bitmapBounds.top; y < bitmapBounds.top + h; y += LINE_SIZE) {
            boolean white = isRectWhite(bmp, bitmapBounds.left + 20, y, bitmapBounds.right - 20, y + LINE_SIZE, avgLum);
            if (white) {
                whiteCount++;
            } else {
                if (whiteCount >= 1) {
                    return (float) (Math.max(bitmapBounds.top, y - LINE_SIZE) - bitmapBounds.top) / bitmapBounds.height();
                }
                whiteCount = 0;
            }
        }
        return whiteCount > 0 ? (float) (Math.max(bitmapBounds.top, y - LINE_SIZE) - bitmapBounds.top) / bitmapBounds.height() : 0;
    }

    private static float getRightBound(BitmapRef bitmap, Rect bitmapBounds, float avgLum) {
        Bitmap bmp = bitmap.getBitmap();
        final int w = bmp.getWidth() / 3;
        int whiteCount = 0;
        int x = 0;
        for (x = bitmapBounds.right - LINE_SIZE; x > bitmapBounds.right - w; x -= LINE_SIZE) {
            boolean white = isRectWhite(bmp, x, bitmapBounds.top + 20, x + LINE_SIZE, bitmapBounds.bottom - 20, avgLum);
            if (white) {
                whiteCount++;
            } else {
                if (whiteCount >= 1) {
                    return (float) (Math.min(bitmapBounds.right, x + 2 * LINE_SIZE) - bitmapBounds.left) / bitmapBounds.width();
                }
                whiteCount = 0;
            }
        }
        return whiteCount > 0 ? (float) (Math.min(bitmapBounds.right, x + 2 * LINE_SIZE) - bitmapBounds.left) / bitmapBounds.width() : 1;
    }

    private static float getBottomBound(BitmapRef bitmap, Rect bitmapBounds, float avgLum) {
        Bitmap bmp = bitmap.getBitmap();
        final int h = bmp.getHeight() / 3;
        int whiteCount = 0;
        int y = 0;
        for (y = bitmapBounds.bottom - LINE_SIZE; y > bitmapBounds.bottom - h; y -= LINE_SIZE) {
            boolean white = isRectWhite(bmp, bitmapBounds.left + 20, y, bitmapBounds.right - 20, y + LINE_SIZE, avgLum);
            if (white) {
                whiteCount++;
            } else {
                if (whiteCount >= 1) {
                    return (float) (Math.min(bitmapBounds.bottom, y + 2 * LINE_SIZE) - bitmapBounds.top) / bitmapBounds.height();
                }
                whiteCount = 0;
            }
        }
        return whiteCount > 0 ? (float) (Math.min(bitmapBounds.bottom, y + 2 * LINE_SIZE) - bitmapBounds.top) / bitmapBounds.height() : 1;
    }

    private static boolean isRectWhite(Bitmap bmp, int l, int t, int r, int b, float avgLum) {
        int count = 0;
        for (int x = l; x < r; x++) {
            for (int y = t; y < b; y++) {
                int c = bmp.getPixel(x, y);

                float lum = getLum(c);
                if ((lum < avgLum) && ((avgLum - lum) * 10 > avgLum)) {
                    count++;
                }

            }
        }
        return ((float) count / ((r - l) * (b - t))) < 0.005;
    }

    private static float calculateAvgLum(BitmapRef bitmap, Rect bitmapBounds) {
        Bitmap bmp = bitmap.getBitmap();
        if (bmp == null) {
            return 1000;
        }
        float lum = 0f;
        int sizeX = bitmapBounds.width() / 10;
        int sizeY = bitmapBounds.height() / 10;
        int count = 0;
        for (int x = bitmapBounds.left + bitmapBounds.width() / 2 - sizeX; x < bitmapBounds.left + bitmapBounds.width()
                / 2 + sizeX; x++) {
            for (int y = bitmapBounds.top + bitmapBounds.height() / 2 - sizeY; y < bitmapBounds.top
                    + bitmapBounds.height() / 2 + sizeY; y++) {
                int c = bmp.getPixel(x, y);

                lum += getLum(c);
                count++;
            }
        }

        return lum / (count);
    }

    private static float getLum(int c) {
        int r = (c & 0xFF0000) >> 16;
        int g = (c & 0xFF00) >> 8;
        int b = c & 0xFF;

        int min = Math.min(r, Math.min(g, b));
        int max = Math.max(r, Math.max(g, b));
        return (min + max) / 2;
    }

}
