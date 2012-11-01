package org.emdev.common.textmarkup.line;


import android.graphics.Canvas;


public class LineFixedWhiteSpace extends AbstractLineElement {

    public LineFixedWhiteSpace(final float width, final int height) {
        super(width, height, true);
    }

    @Override
    public float render(final Canvas c, final int y, final int x, final float additionalWidth, float left, float right, final int nightmode) {
        return width;
    }
}
