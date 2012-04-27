package org.ebookdroid.droids.fb2.codec;

import org.ebookdroid.droids.fb2.codec.FB2Document.LineCreationParams;

import java.util.ArrayList;

public class FB2MarkupImageRef implements FB2MarkupElement {

    private final String ref;
    private final boolean inline;

    public FB2MarkupImageRef(final String name, final boolean inline) {
        this.ref = name;
        this.inline = inline;
    }

    @Override
    public void publishToLines(ArrayList<FB2Line> lines, LineCreationParams params) {
        final FB2Image image = params.doc.getImage(ref, inline);
        if (image != null) {
            if (!inline) {
                final FB2Line line = new FB2Line(params.maxLineWidth, params.jm);
                line.append(image);
                line.applyJustification(JustificationMode.Center);
                lines.add(line);
            } else {
                image.publishToLines(lines, params);
            }
        }
    }

}
