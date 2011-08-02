package org.ebookdroid.core;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelXorXfermode;
import android.text.TextPaint;

public enum PagePaint {
    DAY(Color.BLACK, Color.WHITE, false),

    NIGHT(Color.WHITE, Color.BLACK, true);

    private final Paint bitmapPaint;
    private final TextPaint textPaint = new TextPaint();
    private final Paint fillPaint = new Paint();
    private final Paint strokePaint = new Paint();

    private PagePaint(final int textColor, final int fillColor, final boolean invert) {
        if (invert) {
            bitmapPaint = new Paint();
            bitmapPaint.setXfermode(new PixelXorXfermode(-1));
        } else {
            bitmapPaint = null;
        }

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
