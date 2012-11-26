package org.emdev.common.textmarkup.line;

import org.emdev.common.textmarkup.RenderingStyle;

public class TextPreElement extends TextElement {

    public TextPreElement(TextElement original, int st, int len, float width) {
        super(original, st, len, width);
    }

    public TextPreElement(char[] ch, int st, int len, RenderingStyle style) {
        super(ch, st, len, style);
    }

    @Override
    public AbstractLineElement[] split(final float remaining, boolean hyphenEnabled) {
        if (!hyphenEnabled) {
            return null;
        }
        final int firstStart = this.start;
        int firstLen = 0;
        int secondStart = this.start;

        float summ = 0;

        for (int i = this.start; i < this.start + this.length; i++) {
            final float width = style.paint.measureText(chars, i, 1);
            final float total = summ + width;
            if (total > remaining) {
                break;
            }
            summ = total;
            firstLen++;
            secondStart++;
        }

        if (secondStart == firstStart) {
            return null;
        }

        final int secondLength = this.length - firstLen;

        final TextPreElement first = new TextPreElement(this, firstStart, firstLen, summ);
        final TextPreElement second = new TextPreElement(this, secondStart, secondLength, this.width - summ);

        final AbstractLineElement[] result = { first, second };
        return result;
    }
}
