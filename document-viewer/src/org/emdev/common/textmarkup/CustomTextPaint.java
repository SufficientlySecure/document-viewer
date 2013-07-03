package org.emdev.common.textmarkup;

import org.ebookdroid.droids.fb2.codec.FB2Page;

import android.graphics.Paint;
import android.text.TextPaint;

import org.emdev.common.fonts.typeface.TypefaceEx;
import org.emdev.common.textmarkup.line.LineFixedWhiteSpace;
import org.emdev.common.textmarkup.line.LineWhiteSpace;

public class CustomTextPaint extends TextPaint {

    private static final ThreadLocal<char[]> chars = new ThreadLocal<char[]>();

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

    public CustomTextPaint(final int key, final TypefaceEx face, final int textSize) {
        super();
        this.key = key;
        setTextSize(textSize);
        setTypeface(face.typeface);
        setFakeBoldText(face.fakeBold);
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

    public CustomTextPaint(final Paint parent, final int key, final TypefaceEx face, final int textSize) {
        super(parent);
        this.key = key;
        setTextSize(textSize);
        setTypeface(face.typeface);
        setFakeBoldText(face.fakeBold);
        setAntiAlias(parent.isAntiAlias());
        setFilterBitmap(parent.isFilterBitmap());
        setDither(parent.isDither());

        initMeasures();

        space = new LineWhiteSpace(standard[0x20], textSize);
        fixedSpace = new LineFixedWhiteSpace(standard[0x20], textSize);
        emptyLine = new LineFixedWhiteSpace(FB2Page.PAGE_WIDTH - 2 * FB2Page.MARGIN_X, textSize);
        pOffset = new LineFixedWhiteSpace(textSize * 3, textSize);
        vOffset = new LineFixedWhiteSpace(FB2Page.PAGE_WIDTH / 8, textSize);
    }

    private void initMeasures() {
        initMeasures(0x0000, standard, 0x20, 0xFF);
        initMeasures(0x0100, latin1);
        initMeasures(0x0400, rus);
        initMeasures(0x2000, punct, 0x00, 0x0B);
        initMeasures(0x2000, punct, 0x10, 0x27);
        initMeasures(0x2000, punct, 0x30, 0x3F);
        initMeasures(0x2000, punct, 0x40, 0x4F);
        initMeasures(0x2000, punct, 0x50, 0x5F);
        initMeasures(0x2000, punct, 0xD0, 0xDF);
        initMeasures(0x2000, punct, 0xE0, 0xEF);
        initMeasures(0x2000, punct, 0xF0, 0xF0);
    }

    private void initMeasures(final int codepage, final float[] widths) {
        final char[] chars = new char[256];
        for (int i = 0; i < 256; i++) {
            chars[i] = (char) (i + codepage);
        }
        this.getTextWidths(chars, 0, chars.length, widths);
    }

    private void initMeasures(final int codepage, final float[] widths, final int start, final int end) {
        final int length = end - start + 1;
        final char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = (char) (i + start + codepage);
        }
        final float[] w = new float[length];
        this.getTextWidths(chars, 0, length, w);
        System.arraycopy(w, 0, widths, start, length);
    }

    @Override
    public float measureText(final char[] text, final int index, final int count) {
        float sum = 0;
        int tempIndex = 0;
        char[] temp = chars.get();
        for (int i = index, n = 0; n < count; i++, n++) {
            final char ch = text[i];
            final int code = ch;
            final int enc = code & 0xFFFFFF00;
            switch (enc) {
                case 0x0000:
                    sum += standard[code & 0x00FF];
                    break;
                case 0x0100:
                    sum += latin1[code & 0x00FF];
                    break;
                case 0x0400:
                    sum += rus[code & 0x00FF];
                    break;
                case 0x2000:
                    float w = punct[code & 0x00FF];
                    if (w == 0) {
                        w = punct[code & 0x00FF] = super.measureText(text, i, 1);
                        if (w == 0) {
                            w = punct[code & 0x00FF] = standard[0x20];
                        }
                    }
                    sum += w;
                    break;
                default:
                    if (temp == null) {
                        temp = new char[256];
                        chars.set(temp);
                    }
                    if (tempIndex < temp.length) {
                        temp[tempIndex++] = ch;
                    } else {
                        sum += super.measureText(temp, 0, temp.length);
                        tempIndex = 0;
                    }
            }
        }
        if (tempIndex > 0) {
            sum += super.measureText(temp, 0, tempIndex);
        }
        return sum;
    }
}
