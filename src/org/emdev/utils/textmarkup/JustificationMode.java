package org.emdev.utils.textmarkup;


import org.ebookdroid.droids.fb2.codec.LineCreationParams;

import java.util.ArrayList;

import org.emdev.utils.textmarkup.line.Line;


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
    public void publishToLines(ArrayList<Line> lines, LineCreationParams params) {
        params.jm = this;
    }
}
