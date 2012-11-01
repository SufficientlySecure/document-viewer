package org.emdev.common.textmarkup;

import org.emdev.common.textmarkup.line.LineStream;

public class MarkupParagraphEnd implements MarkupElement {

    public static final MarkupParagraphEnd E = new MarkupParagraphEnd();

    private MarkupParagraphEnd() {
    }

    @Override
    public void publishToLines(final LineStream lines) {
        lines.commitParagraph();
    }

}
