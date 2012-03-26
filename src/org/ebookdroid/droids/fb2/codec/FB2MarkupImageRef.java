package org.ebookdroid.droids.fb2.codec;

public class FB2MarkupImageRef implements FB2MarkupElement {

    private final String ref;
    private final boolean inline;

    public FB2MarkupImageRef(final String name, final boolean inline) {
        this.ref = name;
        this.inline = inline;
    }

    @Override
    public void publishToDocument(final FB2Document doc) {
        doc.publishImage(ref, inline);
    }

}
