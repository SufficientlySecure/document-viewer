package org.emdev.utils.textmarkup.line;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;


public class HorizontalRule extends AbstractLineElement {

    private static Paint rulePaint;

    public HorizontalRule(final int width, final int height) {
        super(width, height);
        if (rulePaint == null) {
            rulePaint = new Paint();
            rulePaint.setColor(Color.BLACK);
        }
    }

    @Override
    public float render(final Canvas c, final int y, final int x, final float additionalWidth, float left, float right) {
        if (left < x + width && x < right) {
            c.drawLine(x, y - height / 2, x + width, y - height / 2, rulePaint);
            c.drawRect(x, y - height / 2, x + width, y - height / 2 + 1, rulePaint);
        }
        return width;
    }
}
