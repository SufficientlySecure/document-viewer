package org.emdev.common.textmarkup;

import org.emdev.common.textmarkup.line.Image;
import org.emdev.common.textmarkup.line.Line;
import org.emdev.common.textmarkup.line.LineStream;

public class MarkupImageRef implements MarkupElement {

    private final String ref;
    private final boolean inline;

    public MarkupImageRef(final String name, final boolean inline) {
        this.ref = name;
        this.inline = inline;
    }

    @Override
    public void publishToLines(final LineStream lines) {
        final Image image = lines.params.content.getImage(ref, inline);
        if (image != null) {
            if (!inline) {
                final Line line = lines.add();
                line.append(image);
                line.applyJustification(JustificationMode.Center);
            } else {
                image.publishToLines(lines);
            }
        }
    }

}
