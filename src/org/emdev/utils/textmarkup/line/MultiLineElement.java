package org.emdev.utils.textmarkup.line;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

import java.util.List;

import org.emdev.utils.LengthUtils;

public class MultiLineElement extends AbstractLineElement {

    public static final int BORDER_WIDTH = 3;

    private List<Line> lines;
    private boolean hasBorder;

    private boolean hasBackground;
    final static Paint paint = new Paint();

    public MultiLineElement(int cellWidth, int maxHeight, List<Line> cellLines, boolean hasBorder,
            boolean hasBackground) {
        super(cellWidth, maxHeight);
        this.lines = cellLines;
        this.hasBorder = hasBorder;
        this.hasBackground = hasBackground;
    }

    public static int calcHeight(List<Line> cellLines) {
        int h = 0;
        if (LengthUtils.isNotEmpty(cellLines)) {
            for (Line line : cellLines) {
                h += line.getHeight();
            }
        }
        return h;
    }

    @Override
    public float render(Canvas c, int y, int x, float additionalWidth, float left, float right) {
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
            for (Line line : lines) {
                line.render(c, x + BORDER_WIDTH, y1 + line.getHeight(), left, right);
                y1 += line.getHeight();
            }
        }
        return width;
    }

    public void applyNotes(Line line) {
        if (LengthUtils.isNotEmpty(lines)) {
            for (Line l : lines) {
                final List<Line> notes = l.getFootNotes();
                if (LengthUtils.isNotEmpty(notes)) {
                    notes.remove(0);
                    line.addNote(notes);
                }
            }
        }
    }


}
