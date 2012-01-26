package org.ebookdroid.fb2droid.codec;

import org.ebookdroid.fb2droid.codec.RenderingStyle.Script;
import org.ebookdroid.fb2droid.codec.RenderingStyle.Strike;

import android.graphics.Canvas;

public class FB2TextElement extends AbstractFB2LineElement {

    public final char[] chars;
    public final int start;
    public final int length;
    public final int offset;

    public final RenderingStyle style;

    public FB2TextElement(final char[] ch, final int st, final int len, final RenderingStyle style) {
        super(style.paint.measureText(ch, st, len), style.textSize);
        this.chars = ch;
        this.start = st;
        this.length = len;
        this.style = style;
        this.offset = style.script == Script.SUPER ? (- style.textSize) : style.script == Script.SUB ? style.textSize / 2 : 0;
    }

    @Override
    public float render(final Canvas c, final int y, final int x, final float additionalWidth, float left, float right) {
        if (left < x + width && x < right) {
            int yy = y + offset;
            c.drawText(chars, start, length, x, yy, style.paint);
            if (style.strike == Strike.THROUGH) {
                c.drawLine(x, yy - style.textSize / 4, x + width, yy - style.textSize / 4, style.paint);
                c.drawRect(x, yy - style.textSize / 4, x + width, yy - style.textSize / 4 + 1, style.paint);
            }
        }
        return width;
    }
}
