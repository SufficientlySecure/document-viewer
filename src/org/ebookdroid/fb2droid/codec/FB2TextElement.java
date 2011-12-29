package org.ebookdroid.fb2droid.codec;

import android.graphics.Canvas;

public class FB2TextElement extends AbstractFB2LineElement {

    public final char[] chars;
    public final int start;
    public final int length;

    public final RenderingStyle renderingState;

    public FB2TextElement(final char[] ch, final int st, final int len, final RenderingStyle renderingState) {
        super(renderingState.getTextPaint().measureText(ch, st, len), renderingState.textSize);
        this.chars = ch;
        this.start = st;
        this.length = len;
        this.renderingState = renderingState;
    }

    @Override
    public float render(final Canvas c, final int y, final int x, final float additionalWidth) {
        c.drawText(chars, start, length, x, renderingState.superScript ? y - renderingState.textSize
                : renderingState.subScript ? y + renderingState.textSize / 2 : y, renderingState.getTextPaint());
        return width;
    }
}
