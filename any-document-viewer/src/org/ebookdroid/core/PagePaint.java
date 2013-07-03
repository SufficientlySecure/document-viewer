package org.ebookdroid.core;

import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;

public enum PagePaint {

    /**
     *
     */
    DAY(Color.BLACK, Color.WHITE, Color.BLACK),
    /**
     *
     */
    NIGHT(Color.WHITE, Color.BLACK, Color.BLACK);

    public final Paint bitmapPaint;
    public final TextPaint textPaint = new TextPaint();
    public final Paint fillPaint = new Paint();
    public final Paint backgroundFillPaint = new Paint();
    public final Paint decodingPaint = new Paint();
    public final Paint strokePaint = new Paint();

    private PagePaint(final int textColor, final int fillColor, final int bgFillPaint) {
        bitmapPaint = new Paint();
        bitmapPaint.setFilterBitmap(false);

        textPaint.setColor(textColor);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(24);
        textPaint.setTextAlign(Paint.Align.CENTER);

        fillPaint.setColor(fillColor);
        fillPaint.setStyle(Paint.Style.FILL);

        backgroundFillPaint.setColor(bgFillPaint);
        backgroundFillPaint.setStyle(Paint.Style.FILL);

        decodingPaint.setColor(Color.GRAY);
        decodingPaint.setStyle(Paint.Style.FILL);

        strokePaint.setColor(textColor);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2);
    }
}
