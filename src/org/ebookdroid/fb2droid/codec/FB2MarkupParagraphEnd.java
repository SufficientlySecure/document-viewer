package org.ebookdroid.fb2droid.codec;

public class FB2MarkupParagraphEnd implements FB2MarkupElement {

    public static final FB2MarkupParagraphEnd E = new FB2MarkupParagraphEnd();

    private FB2MarkupParagraphEnd() {
    }

    @Override
    public void publishToDocument(final FB2Document doc) {
        doc.commitParagraph();
    }

}
