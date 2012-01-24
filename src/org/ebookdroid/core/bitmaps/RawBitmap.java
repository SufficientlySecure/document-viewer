package org.ebookdroid.core.bitmaps;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public final class RawBitmap {

    private final int[] pixels;
    private int width;
    private int height;
    private final boolean hasAlpha;

    public RawBitmap(int width, int height, boolean hasAlpha) {
        this.width = width;
        this.height = height;
        this.hasAlpha = hasAlpha;
        this.pixels = new int[width * height];
    }

    public RawBitmap(Bitmap bitmap, Rect srcRect) {
        width = srcRect.width();
        height = srcRect.height();
        hasAlpha = bitmap.hasAlpha();
        pixels = new int[width * height];

        bitmap.getPixels(pixels, 0, width, srcRect.left, srcRect.top, width, height);
    }

    public RawBitmap(Bitmap bitmap, int left, int top, int width, int height) {
        this.width = width;
        this.height = height;
        hasAlpha = bitmap.hasAlpha();
        pixels = new int[width * height];

        bitmap.getPixels(pixels, 0, width, left, top, width, height);
    }

    public void retrieve(Bitmap bitmap, int left, int top, int width, int height) {
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

    public void draw(Canvas canvas, float x, float y, Paint paint) {
        canvas.drawBitmap(pixels, 0, width, x, y, width, height, hasAlpha, paint);
    }

    public void toBitmap(Bitmap bitmap) {
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }

    public Bitmap toBitmap() {
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    public boolean hasAlpha() {
        return hasAlpha;
    }

    public void fillAlpha(int v) {
        for (int i = 0; i < pixels.length; ++i) {
            pixels[i] = (0x00ffffff & pixels[i]) | (v << 24);
        }
    }

    public void invert() {
        nativeInvert(pixels, width, height);
    }

    public Bitmap scaleHq4x() {
        return scaleHq4x(this);
    }

    public Bitmap scaleHq3x() {
        return scaleHq3x(this);
    }

    public Bitmap scaleHq2x() {
        return scaleHq2x(this);
    }

    public static Bitmap scaleHq4x(RawBitmap src) {
        RawBitmap dest = new RawBitmap(src.getWidth() * 4, src.getHeight() * 4, src.hasAlpha());
        src.fillAlpha(0x00);

        nativeHq4x(src.getPixels(), dest.getPixels(), src.getWidth(), src.getHeight());
        dest.fillAlpha(0xFF);
        return dest.toBitmap();
    }

    public static Bitmap scaleHq3x(RawBitmap src) {
        RawBitmap dest = new RawBitmap(src.getWidth() * 3, src.getHeight() * 3, src.hasAlpha());
        src.fillAlpha(0x00);

        nativeHq3x(src.getPixels(), dest.getPixels(), src.getWidth(), src.getHeight());
        dest.fillAlpha(0xFF);
        return dest.toBitmap();
    }

    public static Bitmap scaleHq2x(RawBitmap src) {
        RawBitmap dest = new RawBitmap(src.getWidth() * 2, src.getHeight() * 2, src.hasAlpha());
        src.fillAlpha(0x00);

        nativeHq2x(src.getPixels(), dest.getPixels(), src.getWidth(), src.getHeight());
        dest.fillAlpha(0xFF);
        return dest.toBitmap();
    }

    private static native void nativeHq2x(int[] src, int[] dst, int width, int height);

    private static native void nativeHq3x(int[] src, int[] dst, int width, int height);

    private static native void nativeHq4x(int[] src, int[] dst, int width, int height);

    private static native void nativeInvert(int[] src, int width, int height);

}
