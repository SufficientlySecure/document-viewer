package org.emdev.common.textmarkup;

import org.ebookdroid.droids.fb2.codec.FB2Page;
import org.ebookdroid.droids.fb2.codec.LineCreationParams;

import java.util.ArrayList;

import org.emdev.common.textmarkup.line.Line;

public class MarkupEndPage extends Line implements MarkupElement {

    public static final MarkupElement E = new MarkupEndPage();

    private MarkupEndPage() {
        super(0, JustificationMode.Left);
    }

    @Override
    public void publishToLines(final ArrayList<Line> lines, final LineCreationParams params) {
        if (!lines.isEmpty() && lines.get(lines.size() - 1) == E) {
            return;
        }
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
