package org.emdev.common.textmarkup;

import org.ebookdroid.droids.fb2.codec.LineCreationParams;

import java.util.ArrayList;

import org.emdev.common.textmarkup.line.Line;


public class MarkupExtraSpace implements MarkupElement {

    private final int extraSpace;
    
    public MarkupExtraSpace(int extraSpace) {
        this.extraSpace = extraSpace;
    }
    
    @Override
    public void publishToLines(ArrayList<Line> lines, LineCreationParams params) {
        params.extraSpace += this.extraSpace;
    }

}
