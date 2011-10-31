package org.ebookdroid.fb2droid.codec;

public enum JustificationMode implements FB2MarkupElement {
    /**
     * 
     */
    Center,
    /**
     * 
     */
    Left,
    /**
     * 
     */
    Right,
    /**
     * 
     */
    Justify;

    @Override
    public void publishToDocument(final FB2Document doc) {
        doc.jm = this;
    }
}
