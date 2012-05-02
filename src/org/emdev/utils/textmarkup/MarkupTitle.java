package org.emdev.utils.textmarkup;


import org.ebookdroid.droids.fb2.codec.LineCreationParams;

import java.util.ArrayList;

import org.emdev.utils.textmarkup.line.Line;


public class MarkupTitle implements MarkupElement {

    public final String title;
    public final int level;

    public MarkupTitle(final String string, int level) {
        this.title = string;
        this.level = level;
    }

    @Override
    public void publishToLines(ArrayList<Line> lines, LineCreationParams params) {
        Line.getLastLine(lines, params.maxLineWidth, params.jm).setTitle(this);
    }

}
