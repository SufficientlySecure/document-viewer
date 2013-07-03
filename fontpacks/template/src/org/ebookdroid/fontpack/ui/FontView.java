package org.ebookdroid.fontpack.ui;

import java.util.ArrayList;
import java.util.List;

import org.ebookdroid.fontpack.FontpackApp;
import org.ebookdroid.fontpack.R;
import org.emdev.common.fonts.SystemFontProvider;
import org.emdev.common.fonts.data.FontFamily;
import org.emdev.common.fonts.data.FontFamilyType;
import org.emdev.common.fonts.data.FontInfo;
import org.emdev.common.fonts.data.FontPack;
import org.emdev.common.fonts.data.FontStyle;
import org.emdev.common.fonts.typeface.TypefaceEx;
import org.emdev.utils.MathUtils;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class FontView extends View {

    final List<Line> lines = new ArrayList<Line>();

    public FontView(final Context context) {
        super(context);
        init();
    }

    public FontView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FontView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    protected void init() {
        final TypefaceEx system = FontpackApp.sfm.getFontPack(SystemFontProvider.SYSTEM_FONT_PACK).getTypeface(
                FontFamilyType.SANS, FontStyle.REGULAR);

        final int textSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, getResources()
                .getDisplayMetrics());
        final int offsetX = textSize;
        final int offsetY = textSize;

        final int interval1 = textSize / 2;
        final int interval2 = textSize;

        final float x = offsetX;
        float y = offsetY;

        float maxWidth = 0;

        for (final FontPack fp : FontpackApp.afm) {
            for (final FontFamily fm : fp) {
                for (final FontInfo fi : fm) {
                    final TypefaceEx font = fp.getTypeface(fm.type, fi.style);
                    final String name = fp.name + ": " + fm.type.getResValue() + " " + fi.style.getResValue();
                    final String value = getTextString(fm.type);

                    final Line nameLine = new Line(x, y, textSize, system, name);
                    lines.add(nameLine);
                    y += nameLine.height + interval1;

                    final Line valueLine = new Line(x, y, textSize, font, value);
                    lines.add(valueLine);
                    y += valueLine.height + interval2;

                    maxWidth = MathUtils.fmax(maxWidth, nameLine.width, valueLine.width);
                }
            }
        }

        setMinimumWidth((int) (2 * offsetX + maxWidth));
        setMinimumHeight((int) (y + offsetY));
    }

    protected String getTextString(final FontFamilyType type) {
        String text = "";
        switch (type) {
            case MONO:
            case SANS:
            case SERIF:
                text = getContext().getString(R.string.text_string);
                break;
            case SYMBOL:
                for (int ch = 0x21; ch <= 0x2F; ch++) {
                    text = text + (char) ch;
                }
                break;
            case DINGBAT:
                for (int ch = 0x2701; ch <= 0x270F; ch++) {
                    text = text + (char) ch;
                }
                break;
        }
        return text;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        for (final Line line : lines) {
            line.draw(canvas);
        }

    }

    private static final class Line {

        final float x;
        final float y;
        final float width;
        final float height;
        final TextPaint paint;
        final String text;

        Line(final float x, final float y, final int textSize, final TypefaceEx font, final String text) {
            this.x = x;
            this.y = y;
            this.text = text;

            this.paint = new TextPaint();
            this.paint.setAntiAlias(true);
            this.paint.setFilterBitmap(true);
            this.paint.setDither(true);

            this.paint.setTextSize(textSize);
            this.paint.setTypeface(font.typeface);
            this.paint.setFakeBoldText(font.fakeBold);

            this.width = paint.measureText(text);
            this.height = paint.getTextSize();
        }

        public void draw(final Canvas canvas) {
            canvas.drawText(text, x, y + height, paint);
        }

    }

}
