package org.ebookdroid.fb2droid.codec;

import android.text.TextPaint;

public class CustomTextPaint extends TextPaint {

    public float spaceSize;

    private final float[] standard = new float[256];
    private final float[] latin1 = new float[256];
    private final float[] rus = new float[256];
    private final float[] punct = new float[256];

    public void initMeasures() {
        final char[] chars = { ' ' };
        for (int i = 0; i < 256; i++) {
            chars[0] = (char) i;
            standard[i] = super.measureText(chars, 0, 1);
            chars[0] = (char) (i + 0x0100);
            latin1[i] = super.measureText(chars, 0, 1);
            chars[0] = (char) (i + 0x0400);
            rus[i] = super.measureText(chars, 0, 1);
            chars[0] = (char) (i + 0x2000);
            punct[i] = super.measureText(chars, 0, 1);
        }
        spaceSize = standard[0x20];
    }

    @Override
    public float measureText(final char[] text, final int index, final int count) {
        float sum = 0;
        for (int i = index, n = 0; n < count; i++, n++) {
            int ch = (int)text[i];
            final int enc = ch & 0x0000FF00;
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
                    System.out.println("CTP: unknown symbol: " + text[i] + " " + Integer.toHexString(ch));
                    return super.measureText(text, index, count);
            }
        }
        return sum;
    }

}
