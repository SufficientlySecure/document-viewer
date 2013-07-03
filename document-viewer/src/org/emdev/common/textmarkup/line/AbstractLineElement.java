package org.emdev.common.textmarkup.line;

import android.graphics.RectF;

import org.emdev.common.textmarkup.RenderingStyle;

public abstract class AbstractLineElement implements LineElement {

    public final int height;
    public float width;

    public final boolean isWhiteSpace;

    public AbstractLineElement(final float width, final int height, final boolean isWS) {
        this.height = height;
        this.width = width;
        this.isWhiteSpace = isWS;
    }

    public AbstractLineElement(final RectF rect) {
        this(rect.width(), (int) rect.height(), false);
    }

    @Override
    public final void publishToLines(final LineStream lines) {
        Line line = lines.tail();
        final int h = Math.max(line.getHeight(), height);
        final LineWhiteSpace space = RenderingStyle.getTextPaint(lines.params.content, h).space;
        float remaining = lines.params.maxLineWidth - (line.width + (isWhiteSpace ? 0 : space.width));

        if (remaining <= 0 && !lines.params.noLineBreak) {
            line = lines.add();
            remaining = lines.params.maxLineWidth;
        }

        if (this.width <= remaining || lines.params.noLineBreak) {
            if (line.hasNonWhiteSpaces() && lines.params.insertSpace && !isWhiteSpace) {
                line.append(space);
            }
            line.append(this);
        } else {
            final AbstractLineElement[] splitted = split(remaining, lines.params.hyphenEnabled);
            if (splitted != null && splitted.length > 1) {
                if (line.hasNonWhiteSpaces() && lines.params.insertSpace && !isWhiteSpace) {
                    line.append(space);
                }
                for (int i = 0; i < splitted.length - 1; i++) {
                    line.append(splitted[i]);
                }
            }

            line = lines.add();

            if (splitted == null) {
                line.append(this);
            } else {
                splitted[splitted.length - 1].publishToLines(lines);
            }
        }
        lines.params.insertSpace = !isWhiteSpace;
        lines.params.noLineBreak = false;
    }

    public AbstractLineElement[] split(final float remaining, boolean hyphenEnabled) {
        return null;
    }
}
