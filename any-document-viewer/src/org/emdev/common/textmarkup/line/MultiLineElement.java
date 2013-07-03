package org.emdev.common.textmarkup.line;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

import org.emdev.utils.LengthUtils;

public class MultiLineElement extends AbstractLineElement {

    public static final int BORDER_WIDTH = 3;

    private final LineStream lines;
    private final boolean hasBorder;

    private final boolean hasBackground;
    final static Paint paint = new Paint();

    public MultiLineElement(final int cellWidth, final int maxHeight, final LineStream cellLines,
            final boolean hasBorder, final boolean hasBackground) {
        super(cellWidth, maxHeight, false);
        this.lines = cellLines;
        this.hasBorder = hasBorder;
        this.hasBackground = hasBackground;
    }

    public static int calcHeight(final LineStream cellLines) {
        int h = 0;
        if (LengthUtils.isNotEmpty(cellLines)) {
            for (final Line line : cellLines) {
                h += line.getHeight();
            }
        }
        return h;
    }

    @Override
    public float render(final Canvas c, final int y, final int x, final float additionalWidth, final float left,
            final float right, final int nightmode) {
        if (hasBackground) {
            paint.setStyle(Style.FILL);
            paint.setStrokeWidth(0);
            paint.setColor(Color.GRAY);
            c.drawRect(x, y - height, x + width, y, paint);
        }
        if (hasBorder) {
            paint.setStyle(Style.STROKE);
            paint.setStrokeWidth(1);
            paint.setColor(Color.BLACK);
            c.drawRect(x, y - height, x + width, y, paint);
        }
        if (LengthUtils.isNotEmpty(lines)) {
            int y1 = y - height + BORDER_WIDTH;
            for (final Line line : lines) {
                line.render(c, x + BORDER_WIDTH, y1 + line.getHeight(), left, right, nightmode);
                y1 += line.getHeight();
            }
        }
        return width;
    }

    public void applyNotes(final Line line) {
        if (LengthUtils.isNotEmpty(lines)) {
            for (final Line l : lines) {
                final LineStream notes = l.getFootNotes();
                if (LengthUtils.isNotEmpty(notes)) {
                    line.addNote(notes, true);
                }
            }
        }
    }

}
