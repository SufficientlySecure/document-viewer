package org.emdev.common.textmarkup;


import org.ebookdroid.droids.fb2.codec.LineCreationParams;

import java.util.ArrayList;

import org.emdev.common.textmarkup.line.Line;


public class MarkupNoLineBreak implements MarkupElement {

    private MarkupNoLineBreak() {

    }

    public static final MarkupNoLineBreak _instance = new MarkupNoLineBreak();

    @Override
    public void publishToLines(ArrayList<Line> lines, LineCreationParams params) {
        params.noLineBreak = true;
    }

}
