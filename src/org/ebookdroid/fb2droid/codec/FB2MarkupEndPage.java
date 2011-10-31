package org.ebookdroid.fb2droid.codec;

public class FB2MarkupEndPage implements FB2MarkupElement {

    @Override
    public void publishToDocument(final FB2Document doc) {
        doc.commitPage();
    }

}
