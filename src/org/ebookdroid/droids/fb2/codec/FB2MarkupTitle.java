package org.ebookdroid.droids.fb2.codec;

import org.ebookdroid.droids.fb2.codec.FB2Document.LineCreationParams;

import java.util.ArrayList;

public class FB2MarkupTitle implements FB2MarkupElement {

    final String title;
    final int level;

    public FB2MarkupTitle(final String string, int level) {
        this.title = string;
        this.level = level;
    }

    @Override
    public void publishToLines(ArrayList<FB2Line> lines, LineCreationParams params) {
        FB2Line.getLastLine(lines, params.maxLineWidth).setTitle(this);
    }

}
