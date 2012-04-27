package org.ebookdroid.droids.fb2.codec;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.droids.fb2.codec.RenderingStyle.Script;
import org.ebookdroid.droids.fb2.codec.RenderingStyle.Strike;

import android.graphics.Canvas;

import org.emdev.utils.HyphenationUtils;

public class FB2TextElement extends AbstractFB2LineElement {

    private static final int[] starts = new int[100];
    private static final int[] lengths = new int[100];
    private static final float[] parts = new float[100];

    public final char[] chars;
    public final int start;
    public final int length;
    public final int offset;

    public final RenderingStyle style;

    public FB2TextElement(final char[] ch, final int st, final int len, final RenderingStyle style) {
        super(style.paint.measureText(ch, st, len), style.script == Script.SUPER ? style.textSize * 5 / 2 : style.textSize);
        this.chars = ch;
        this.start = st;
        this.length = len;
        this.style = style;
        this.offset = style.script == Script.SUPER ? (-style.textSize)
                : style.script == Script.SUB ? style.textSize / 2 : 0;
    }

    FB2TextElement(final FB2TextElement original, final int st, final int len, final float width) {
        super(width, original.style.textSize);
        this.chars = original.chars;
        this.start = st;
        this.length = len;
        this.style = original.style;
        this.offset = original.offset;
    }

    @Override
    public float render(final Canvas c, final int y, final int x, final float additionalWidth, final float left,
            final float right) {
        if (left < x + width && x < right) {
            final int yy = y + offset;
            c.drawText(chars, start, length, x, yy, style.paint);
            if (style.strike == Strike.THROUGH) {
                c.drawLine(x, yy - style.textSize / 4, x + width, yy - style.textSize / 4, style.paint);
                c.drawRect(x, yy - style.textSize / 4, x + width, yy - style.textSize / 4 + 1, style.paint);
            }
        }
        return width;
    }

    @Override
    public AbstractFB2LineElement[] split(final float remaining) {
        if (!SettingsManager.getAppSettings().fb2HyphenEnabled) {
            return null;
        }
        final int count = HyphenationUtils.hyphenateWord(chars, start, length, starts, lengths);
        if (count == 0) {
            return null;
        }

        final float dwidth = this.style.defis.width;
        final int firstStart = this.start;
        int firstLen = 0;

        float summ = dwidth;
        int next = 0;

        for (; next < parts.length; next++) {
            final float width = style.paint.measureText(chars, starts[next], lengths[next]);
            final float total = summ + width;
            if (total > remaining) {
                break;
            }
            summ = total;
            firstLen += lengths[next];
        }

        if (next == 0) {
            return null;
        }

        final int secondStart = starts[next];
        final int secondLength = this.length - (starts[next] - this.start);

        final FB2TextElement first = new FB2TextElement(this, firstStart, firstLen, summ - dwidth);
        final FB2TextElement second = new FB2TextElement(this, secondStart, secondLength, this.width - first.width);

        final AbstractFB2LineElement[] result = { first, this.style.defis, second };
        return result;
    }

    public int indexOf(final char[] charArray) {
        int start1 = start;
        if (charArray.length > 0) {
            if (charArray.length + start1 - start > length) {
                return -1;
            }
            while (true) {
                int i = start1;
                boolean found = false;
                for (; i < start + length; i++) {
                    if (chars[i] == charArray[0]) {
                        found = true;
                        break;
                    }
                }
                if (!found || charArray.length + i - start > length) {
                    return -1; // handles subCount > count || start >= count
                }
                int o1 = i, o2 = 0;
                while (++o2 < charArray.length && chars[++o1] == charArray[o2]) {
                    // Intentionally empty
                }
                if (o2 == charArray.length) {
                    return i - start;
                }
                start1 = i + 1;
            }
        }
        return (start1 < length || start1 == 0) ? start1 - start : length - start;
    }

    @Override
    public String toString() {
        return new String(chars, start, length);
    }

}
