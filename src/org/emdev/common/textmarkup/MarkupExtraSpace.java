package org.emdev.common.textmarkup;

import org.emdev.common.textmarkup.line.LineStream;

public class MarkupExtraSpace implements MarkupElement {

    private final int extraSpace;

    public MarkupExtraSpace(final int extraSpace) {
        this.extraSpace = extraSpace;
    }

    @Override
    public void publishToLines(final LineStream lines) {
        lines.params.extraSpace += this.extraSpace;
    }

}
