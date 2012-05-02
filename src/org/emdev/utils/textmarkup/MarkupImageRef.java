package org.emdev.utils.textmarkup;


import org.ebookdroid.droids.fb2.codec.LineCreationParams;

import java.util.ArrayList;

import org.emdev.utils.textmarkup.line.Image;
import org.emdev.utils.textmarkup.line.Line;


public class MarkupImageRef implements MarkupElement {

    private final String ref;
    private final boolean inline;

    public MarkupImageRef(final String name, final boolean inline) {
        this.ref = name;
        this.inline = inline;
    }

    @Override
    public void publishToLines(ArrayList<Line> lines, LineCreationParams params) {
        final Image image = params.content.getImage(ref, inline);
        if (image != null) {
            if (!inline) {
                final Line line = new Line(params.maxLineWidth, params.jm);
                line.append(image);
                line.applyJustification(JustificationMode.Center);
                lines.add(line);
            } else {
                image.publishToLines(lines, params);
            }
        }
    }

}
