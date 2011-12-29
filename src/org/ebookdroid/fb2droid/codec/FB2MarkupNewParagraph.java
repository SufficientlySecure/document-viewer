package org.ebookdroid.fb2droid.codec;

public class FB2MarkupNewParagraph implements FB2MarkupElement {

    private final FB2LineFixedWhiteSpace offset;

    public FB2MarkupNewParagraph(final FB2LineFixedWhiteSpace offset) {
        this.offset = offset;
    }

    @Override
    public void publishToDocument(final FB2Document doc) {
        if (doc.jm != JustificationMode.Center) {
            doc.publishElement(offset);
        }
    }

}
