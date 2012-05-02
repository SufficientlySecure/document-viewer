package org.emdev.utils.textmarkup.line;


import android.graphics.Canvas;


public class LineWhiteSpace extends AbstractLineElement {

    public LineWhiteSpace(final float width, final int height) {
        super(width, height);
    }

    @Override
    public float render(final Canvas c, final int y, final int x, final float additionalWidth, float left, float right) {
        return width + additionalWidth;
    }
}
