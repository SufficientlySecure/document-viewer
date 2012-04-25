package org.ebookdroid.droids.fb2.codec;

import org.ebookdroid.droids.fb2.codec.FB2Document.LineCreationParams;

import android.graphics.RectF;

import java.util.ArrayList;

public abstract class AbstractFB2LineElement implements FB2LineElement {

    public final int height;
    public float width;

    public AbstractFB2LineElement(final float width, final int height) {
        this.height = height;
        this.width = width;
    }

    public AbstractFB2LineElement(final RectF rect) {
        this(rect.width(), (int) rect.height());
    }

    @Override
    public void publishToLines(ArrayList<FB2Line> lines, LineCreationParams params) {
        FB2Line line = FB2Line.getLastLine(lines, params.maxLineWidth);
        final FB2LineWhiteSpace space = RenderingStyle.getTextPaint(line.getHeight()).space;
        final float remaining = params.maxLineWidth - (line.width + space.width);
        if (this.width < remaining) {
            if (line.hasNonWhiteSpaces() && params.insertSpace) {
                line.append(space);
            }
            line.append(this);
            params.insertSpace = true;
        } else {
            final AbstractFB2LineElement[] splitted = split(remaining);
            if (splitted != null && splitted.length > 1) {
                if (line.hasNonWhiteSpaces() && params.insertSpace) {
                    line.append(space);
                }
                for (int i = 0; i < splitted.length - 1; i++) {
                    line.append(splitted[i]);
                }
            }

            line = new FB2Line(params.maxLineWidth);
            lines.add(line);

            line.append(splitted == null ? this : splitted[splitted.length - 1]);
            params.insertSpace = true;
        }
    }

    public AbstractFB2LineElement[] split(final float remaining) {
        return null;
    }
}
