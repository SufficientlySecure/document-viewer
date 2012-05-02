package org.emdev.utils.textmarkup;


import org.ebookdroid.droids.fb2.codec.FB2Page;
import org.ebookdroid.droids.fb2.codec.LineCreationParams;

import java.util.ArrayList;

import org.emdev.utils.textmarkup.line.Line;


public class MarkupEndPage extends Line implements MarkupElement {

    public static final MarkupElement E = new MarkupEndPage();

    private MarkupEndPage() {
        super(0, JustificationMode.Left);
    }

    @Override
    public void publishToLines(ArrayList<Line> lines, LineCreationParams params) {
        lines.add(this);
    }

    @Override
    public int getTotalHeight() {
        return 2 * FB2Page.PAGE_HEIGHT; // to be sure
    }

    @Override
    public boolean appendable() {
        return false;
    }


}
