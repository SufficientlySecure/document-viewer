package org.ebookdroid.common.bitmaps;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public final class RawBitmap {

    final int[] pixels;
    int width;
    int height;
    final boolean hasAlpha;

    public RawBitmap(final int width, final int height, final boolean hasAlpha) {
        this.width = width;
        this.height = height;
        this.hasAlpha = hasAlpha;
        this.pixels = new int[width * height];
    }

    public RawBitmap(final Bitmap bitmap, final Rect srcRect) {
        width = srcRect.width();
        height = srcRect.height();
        hasAlpha = bitmap.hasAlpha();
        pixels = new int[width * height];

        bitmap.getPixels(pixels, 0, width, srcRect.left, srcRect.top, width, height);
    }

    public RawBitmap(final Bitmap bitmap, final int left, final int top, final int width, final int height) {
        this.width = width;
        this.height = height;
        hasAlpha = bitmap.hasAlpha();
        pixels = new int[width * height];

        bitmap.getPixels(pixels, 0, width, left, top, width, height);
    }

    public void retrieve(final Bitmap bitmap, final int left, final int top) {
        bitmap.getPixels(pixels, 0, width, left, top, width, height);
    }

    public void retrieve(final Bitmap bitmap, final int left, final int top, final int width, final int height) {
        this.width = width;
        this.height = height;
        bitmap.getPixels(pixels, 0, width, left, top, width, height);
    }

    public int[] getPixels() {
        return pixels;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void draw(final Canvas canvas, final float x, final float y, final Paint paint) {
        canvas.drawBitmap(pixels, 0, width, x, y, width, height, hasAlpha, paint);
    }

    public void toBitmap(final Bitmap bitmap) {
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }

    public BitmapRef toBitmap() {
        final BitmapRef bitmap = BitmapManager.getBitmap("RawBitmap", width, height, Bitmap.Config.RGB_565);
        bitmap.getBitmap().setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    public boolean hasAlpha() {
        return hasAlpha;
    }

    public void fillAlpha(final int v) {
        for (int i = 0; i < pixels.length; ++i) {
            pixels[i] = (0x00ffffff & pixels[i]) | (v << 24);
        }
    }

    public void invert() {
        nativeInvert(pixels, width, height);
    }

    public int getAvgLum() {
        return nativeAvgLum(pixels, width, height);
    }

    public void contrast(final int contrast) {
        nativeContrast(pixels, width, height, contrast * 256 / 100);
    }

    public void exposure(final int exposure) {
        nativeExposure(pixels, width, height, exposure * 128 / 100);
    }

    public void autoLevels() {
        nativeAutoLevels2(pixels, width, height);
    }

    public BitmapRef scaleHq4x() {
        return scaleHq4x(this);
    }

    public BitmapRef scaleHq3x() {
        return scaleHq3x(this);
    }

    public BitmapRef scaleHq2x() {
        return scaleHq2x(this);
    }

    public static BitmapRef scaleHq4x(final RawBitmap src) {
        final RawBitmap dest = new RawBitmap(src.width * 4, src.height * 4, src.hasAlpha);
        src.fillAlpha(0x00);

        nativeHq4x(src.pixels, dest.pixels, src.width, src.height);
        dest.fillAlpha(0xFF);
        return dest.toBitmap();
    }

    public static BitmapRef scaleHq3x(final RawBitmap src) {
        final RawBitmap dest = new RawBitmap(src.width * 3, src.height * 3, src.hasAlpha);
        src.fillAlpha(0x00);

        nativeHq3x(src.pixels, dest.pixels, src.width, src.height);
        dest.fillAlpha(0xFF);
        return dest.toBitmap();
    }

    public static BitmapRef scaleHq2x(final RawBitmap src) {
        final RawBitmap dest = new RawBitmap(src.width * 2, src.height * 2, src.hasAlpha);
        src.fillAlpha(0x00);

        nativeHq2x(src.pixels, dest.pixels, src.width, src.height);
        dest.fillAlpha(0xFF);
        return dest.toBitmap();
    }

    private static native void nativeHq2x(int[] src, int[] dst, int width, int height);

    private static native void nativeHq3x(int[] src, int[] dst, int width, int height);

    private static native void nativeHq4x(int[] src, int[] dst, int width, int height);

    private static native void nativeInvert(int[] src, int width, int height);

    /* contrast value 256 - normal */
    private static native void nativeContrast(int[] src, int width, int height, int contrast);

    /* Exposure correction values -128...+128 */
    private static native void nativeExposure(int[] src, int width, int height, int exposure);

    private static native void nativeAutoLevels(int[] src, int width, int height);

    private static native void nativeAutoLevels2(int[] src, int width, int height);

    private static native int nativeAvgLum(int[] src, int width, int height);

}
