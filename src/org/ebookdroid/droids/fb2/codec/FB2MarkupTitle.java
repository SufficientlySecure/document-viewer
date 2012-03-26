package org.ebookdroid.droids.fb2.codec;

public class FB2MarkupTitle implements FB2MarkupElement {

    final String title;
    final int level;

    public FB2MarkupTitle(final String string, int level) {
        this.title = string;
        this.level = level;
    }

    @Override
    public void publishToDocument(final FB2Document doc) {
        doc.addTitle(this);
    }

}
