package org.emdev.common.textmarkup;


import org.ebookdroid.droids.fb2.codec.LineCreationParams;

import java.util.ArrayList;

import org.emdev.common.textmarkup.line.Line;


public class MarkupNoSpace implements MarkupElement {

    private MarkupNoSpace() {

    }

    public static final MarkupNoSpace _instance = new MarkupNoSpace();

    @Override
    public void publishToLines(ArrayList<Line> lines, LineCreationParams params) {
        params.insertSpace = false;
    }

}
