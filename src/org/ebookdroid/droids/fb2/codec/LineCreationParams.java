package org.ebookdroid.droids.fb2.codec;

import org.emdev.common.textmarkup.JustificationMode;

public class LineCreationParams {

    public final ParsedContent content;
    public JustificationMode jm;
    public int maxLineWidth;
    public boolean insertSpace = true;
    public int extraSpace = 0;
    public boolean noLineBreak = false;

    public LineCreationParams(final ParsedContent content, final int maxLineWidth, final JustificationMode jm) {
        super();
        this.content = content;
        this.maxLineWidth = maxLineWidth;
        this.jm = jm;
    }

}
