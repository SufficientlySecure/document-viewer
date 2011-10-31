package org.ebookdroid.fb2droid.codec;

import android.graphics.Canvas;

public class FB2LineWhiteSpace extends AbstractFB2LineElement {

    public FB2LineWhiteSpace(final float width, final int height, final boolean sizeable) {
        super(width, height, sizeable);
    }

    @Override
    public void render(final Canvas c, final int y, final int x) {
    }

    @Override
    public void adjustWidth(final float w) {
        if (sizeable) {
            width += w;
        }
    }
}
