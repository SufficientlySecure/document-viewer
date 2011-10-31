package org.ebookdroid.fb2droid.codec;

public class FB2MarkupNote implements FB2MarkupElement {

    private final String ref;

    public FB2MarkupNote(final String ref) {
        this.ref = ref;
    }

    @Override
    public void publishToDocument(final FB2Document doc) {
        doc.publishNote(ref);
    }

}
