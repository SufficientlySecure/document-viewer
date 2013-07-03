package org.emdev.common.textmarkup;

import org.emdev.common.textmarkup.line.LineStream;

public class MarkupNoLineBreak implements MarkupElement {

    public static final MarkupNoLineBreak E = new MarkupNoLineBreak();

    private MarkupNoLineBreak() {
    }

    @Override
    public void publishToLines(final LineStream lines) {
        lines.params.noLineBreak = true;
    }

}
