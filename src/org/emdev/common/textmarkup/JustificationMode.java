package org.emdev.common.textmarkup;


import org.emdev.common.textmarkup.line.LineStream;


public enum JustificationMode implements MarkupElement {

    /** Centered line */
    Center,

    /** Left justified */
    Left,

    /** Right justified */
    Right,

    /** Width justified */
    Justify;

    @Override
    public void publishToLines(LineStream lines) {
        lines.params.jm = this;
    }
}
