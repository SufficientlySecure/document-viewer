package org.emdev.common.textmarkup;

import org.emdev.common.textmarkup.line.LineStream;

public class MarkupNoSpace implements MarkupElement {

    public static final MarkupNoSpace E = new MarkupNoSpace();

    private MarkupNoSpace() {
    }

    @Override
    public void publishToLines(final LineStream lines) {
        lines.params.insertSpace = false;
    }

}
