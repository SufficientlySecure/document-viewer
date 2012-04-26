package org.ebookdroid.droids.fb2.codec;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

import java.util.List;

import org.emdev.utils.LengthUtils;


public class FB2MultiLineElement extends AbstractFB2LineElement {

    private List<FB2Line> lines;
    private boolean hasBorder;

    public FB2MultiLineElement(int cellWidth, int maxHeight, List<FB2Line> cellLines, boolean hasBorder, boolean background) {
        super(cellWidth, maxHeight);
        this.lines = cellLines;
        this.hasBorder = hasBorder;
    }

    static int calcHeight(List<FB2Line> cellLines) {
        int h = 0;
        if (LengthUtils.isNotEmpty(cellLines)) {
            for (FB2Line line : cellLines) {
                h += line.getHeight();
            }
        }
        return h;
    }

    @Override
    public float render(Canvas c, int y, int x, float additionalWidth, float left, float right) {
        if (hasBorder) {
            final Paint paint = new Paint();
            paint.setStyle(Style.STROKE);
            paint.setColor(Color.BLACK);
            c.drawRect(x, y - height, x + width, y, paint);
        }
        if (LengthUtils.isNotEmpty(lines)) {
            int y1 = y - height + 10;
            for (FB2Line line : lines) {
                line.render(c, x + 10, y1 + line.getHeight(), left, right);
                y1 += line.getHeight();
            }
        }
        return width;
    }

}
