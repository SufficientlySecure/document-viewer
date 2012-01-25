package org.ebookdroid.fb2droid.codec;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class FB2HorizontalRule extends AbstractFB2LineElement {

    private static Paint rulePaint;

    public FB2HorizontalRule(final int width, final int height) {
        super(width, height);
        if (rulePaint == null) {
            rulePaint = new Paint();
            rulePaint.setColor(Color.BLACK);
        }
    }

    @Override
    public float render(final Canvas c, final int y, final int x, final float additionalWidth, float left, float right) {
        if (left < x + width && x < right) {
            c.drawRect(x, y - height / 2, x + width, y - height / 2 + 1, rulePaint);
        }
        return width;
    }
}
