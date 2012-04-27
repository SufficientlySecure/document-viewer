package org.ebookdroid.droids.fb2.codec;

import org.ebookdroid.droids.fb2.codec.FB2Document.LineCreationParams;

import java.util.ArrayList;

public class FB2MarkupEndPage extends FB2Line implements FB2MarkupElement {

    public static final FB2MarkupElement E = new FB2MarkupEndPage();

    private FB2MarkupEndPage() {
        super(0, JustificationMode.Left);
    }

    @Override
    public void publishToLines(ArrayList<FB2Line> lines, LineCreationParams params) {
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
