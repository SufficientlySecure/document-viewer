package org.ebookdroid.core;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelXorXfermode;
import android.text.TextPaint;

public enum PagePaint {
    DAY(Color.BLACK, Color.WHITE),

    NIGHT(Color.WHITE, Color.BLACK);

    private final Paint bitmapPaint;
    private final Paint bitmapNightPaint;
    private final TextPaint textPaint = new TextPaint();
    private final Paint fillPaint = new Paint();
    private final Paint strokePaint = new Paint();

    private PagePaint(final int textColor, final int fillColor) {
        bitmapPaint = new Paint();
        bitmapPaint.setFilterBitmap(true);

        bitmapNightPaint = new Paint();
        bitmapNightPaint.setFilterBitmap(true);
        bitmapNightPaint.setXfermode(new PixelXorXfermode(-1));

        textPaint.setColor(textColor);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(24);
        textPaint.setTextAlign(Paint.Align.CENTER);

        fillPaint.setColor(fillColor);
        fillPaint.setStyle(Paint.Style.FILL);

        strokePaint.setColor(textColor);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2);
    }

    public Paint getBitmapPaint() {
        return bitmapPaint;
    }

    public Paint getNightBitmapPaint() {
        return bitmapNightPaint;
    }

    public TextPaint getTextPaint() {
        return textPaint;
    }

    public Paint getFillPaint() {
        return fillPaint;
    }

    public Paint getStrokePaint() {
        return strokePaint;
    }
}
