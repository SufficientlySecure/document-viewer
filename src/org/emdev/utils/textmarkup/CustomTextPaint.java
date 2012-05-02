package org.emdev.utils.textmarkup;

import org.ebookdroid.droids.fb2.codec.FB2Page;

import android.graphics.Typeface;
import android.text.TextPaint;

import org.emdev.utils.textmarkup.line.LineFixedWhiteSpace;
import org.emdev.utils.textmarkup.line.LineWhiteSpace;


public class CustomTextPaint extends TextPaint {

    public final int key;

    public final LineWhiteSpace space;
    public final LineFixedWhiteSpace fixedSpace;
    public final LineFixedWhiteSpace emptyLine;
    public final LineFixedWhiteSpace pOffset;
    public final LineFixedWhiteSpace vOffset;

    private final float[] standard = new float[256];
    private final float[] latin1 = new float[256];
    private final float[] rus = new float[256];
    private final float[] punct = new float[256];

    public CustomTextPaint(final int key, final Typeface face, final int textSize, final boolean bold) {
        this.key = key;
        setTextSize(textSize);
        setTypeface(face);
        setFakeBoldText(bold);
        setAntiAlias(true);
        setFilterBitmap(true);
        setDither(true);

        initMeasures();

        space = new LineWhiteSpace(standard[0x20], textSize);
        fixedSpace = new LineFixedWhiteSpace(standard[0x20], textSize);
        emptyLine = new LineFixedWhiteSpace(FB2Page.PAGE_WIDTH - 2 * FB2Page.MARGIN_X, textSize);
        pOffset = new LineFixedWhiteSpace(textSize * 3, textSize);
        vOffset = new LineFixedWhiteSpace(FB2Page.PAGE_WIDTH / 8, textSize);
    }

    private void initMeasures() {
        initMeasures(0x0000, standard);
        initMeasures(0x0100, latin1);
        initMeasures(0x0400, rus);
        initMeasures(0x2000, punct);
    }

    private void initMeasures(int codepage, float[] widths) {
        final char[] chars = new char[256];
        for (int i = 0; i < 256; i++) {
            chars[i] = (char) (i + codepage);
        }
        this.getTextWidths(chars, 0, chars.length, widths);
    }

    @Override
    public float measureText(final char[] text, final int index, final int count) {
        float sum = 0;
        for (int i = index, n = 0; n < count; i++, n++) {
            final int ch = text[i];
            final int enc = ch & 0xFFFFFF00;
            switch (enc) {
                case 0x0000:
                    sum += standard[ch & 0x00FF];
                    break;
                case 0x0100:
                    sum += latin1[ch & 0x00FF];
                    break;
                case 0x0400:
                    sum += rus[ch & 0x00FF];
                    break;
                case 0x2000:
                    sum += punct[ch & 0x00FF];
                    break;
                default:
                    // System.out.println("CTP: unknown symbol: " + text[i] + " " + Integer.toHexString(ch));
                    sum += super.measureText(text, i, 1);
            }
        }
        return sum;
    }
}
