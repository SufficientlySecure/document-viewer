package org.ebookdroid.fb2droid.codec;

public class FB2MarkupNewParagraph implements FB2MarkupElement {

    private final int textSize;

    public FB2MarkupNewParagraph(final int textSize) {
        this.textSize = textSize;
    }

    @Override
    public void publishToDocument(final FB2Document doc) {
        if (doc.jm != JustificationMode.Center) {
            doc.publishElement(new FB2LineWhiteSpace(textSize * 3, textSize, false));
        }
    }

}
