package org.ebookdroid.fb2droid.codec;

public class FB2MarkupNoSpace implements FB2MarkupElement {

    private FB2MarkupNoSpace() {

    }

    public static final FB2MarkupNoSpace _instance = new FB2MarkupNoSpace();

    @Override
    public void publishToDocument(final FB2Document doc) {
        doc.insertSpace = false;
    }

}
