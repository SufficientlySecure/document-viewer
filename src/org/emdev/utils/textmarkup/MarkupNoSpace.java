package org.emdev.utils.textmarkup;


import org.ebookdroid.droids.fb2.codec.LineCreationParams;

import java.util.ArrayList;

import org.emdev.utils.textmarkup.line.Line;


public class MarkupNoSpace implements MarkupElement {

    private MarkupNoSpace() {

    }

    public static final MarkupNoSpace _instance = new MarkupNoSpace();

    @Override
    public void publishToLines(ArrayList<Line> lines, LineCreationParams params) {
        params.insertSpace = false;
    }

}
