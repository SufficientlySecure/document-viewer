package org.ebookdroid.droids.fb2.codec;

import org.emdev.common.textmarkup.JustificationMode;

public class LineCreationParams {

    public JustificationMode jm;
    public int maxLineWidth;
    public boolean insertSpace = true;
    public ParsedContent content;
    public int extraSpace = 0;
    public boolean noLineBreak = false;
}
