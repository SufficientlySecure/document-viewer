package org.emdev.common.textmarkup;

import org.emdev.common.textmarkup.line.LineStream;

public class MarkupTitle implements MarkupElement {

    public final String title;
    public final int level;

    public MarkupTitle(final String string, final int level) {
        this.title = string;
        this.level = level;
    }

    @Override
    public void publishToLines(final LineStream lines) {
        lines.tail().setTitle(this);
    }

}
