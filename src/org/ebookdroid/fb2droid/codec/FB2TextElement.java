package org.ebookdroid.fb2droid.codec;

import android.graphics.Canvas;

public class FB2TextElement extends AbstractFB2LineElement {

    public final CharSequence chars;
    public final RenderingStyle renderingState;

    public FB2TextElement(final CharSequence ch, final float width, final RenderingStyle renderingState) {
        super(width, renderingState.textSize);
        this.chars = ch;
        this.renderingState = renderingState;
    }

    @Override
    public float render(final Canvas c, final int y, final int x, final float additionalWidth) {
        c.drawText(chars, 0, chars.length(), x, renderingState.superScript ? y - renderingState.textSize
                : renderingState.subScript ? y + renderingState.textSize / 2 : y, renderingState.paint);
        return width;
    }
}
