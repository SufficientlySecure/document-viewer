package org.emdev.common.textmarkup;

import org.emdev.common.textmarkup.line.LineFixedWhiteSpace;
import org.emdev.common.textmarkup.line.LineStream;

public class MarkupNewParagraph implements MarkupElement {

    private final LineFixedWhiteSpace offset;

    public MarkupNewParagraph(final LineFixedWhiteSpace offset) {
        this.offset = offset;
    }

    @Override
    public void publishToLines(final LineStream lines) {
        if (lines.params.jm != JustificationMode.Center && offset != null) {
            offset.publishToLines(lines);
        }
    }

}
