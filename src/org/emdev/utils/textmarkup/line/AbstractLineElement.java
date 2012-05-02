package org.emdev.utils.textmarkup.line;


import org.ebookdroid.droids.fb2.codec.LineCreationParams;

import android.graphics.RectF;

import java.util.ArrayList;

import org.emdev.utils.textmarkup.RenderingStyle;


public abstract class AbstractLineElement implements LineElement {

    public final int height;
    public float width;

    public AbstractLineElement(final float width, final int height) {
        this.height = height;
        this.width = width;
    }

    public AbstractLineElement(final RectF rect) {
        this(rect.width(), (int) rect.height());
    }

    @Override
    public void publishToLines(ArrayList<Line> lines, LineCreationParams params) {
        Line line = Line.getLastLine(lines, params.maxLineWidth, params.jm);
        final LineWhiteSpace space = RenderingStyle.getTextPaint(line.getHeight()).space;
        float remaining = params.maxLineWidth - (line.width + space.width);
        if (remaining <= 0) {
            line = new Line(params.maxLineWidth, params.jm);
            lines.add(line);
            remaining = params.maxLineWidth;
        }
        if (this.width <= remaining) {
            if (line.hasNonWhiteSpaces() && params.insertSpace) {
                line.append(space);
            }
            line.append(this);
        } else {
            final AbstractLineElement[] splitted = split(remaining);
            if (splitted != null && splitted.length > 1) {
                if (line.hasNonWhiteSpaces() && params.insertSpace) {
                    line.append(space);
                }
                for (int i = 0; i < splitted.length - 1; i++) {
                    line.append(splitted[i]);
                }
            }

            line = new Line(params.maxLineWidth, params.jm);
            lines.add(line);

            if (splitted == null) {
                line.append(this);
            } else {
                splitted[splitted.length - 1].publishToLines(lines, params);
            }
        }
        params.insertSpace = true;
    }

    public AbstractLineElement[] split(final float remaining) {
        return null;
    }
}
