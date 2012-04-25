package org.ebookdroid.droids.fb2.codec;

import org.ebookdroid.droids.fb2.codec.FB2Document.LineCreationParams;

import java.util.ArrayList;

public class FB2MarkupNoSpace implements FB2MarkupElement {

    private FB2MarkupNoSpace() {

    }

    public static final FB2MarkupNoSpace _instance = new FB2MarkupNoSpace();

    @Override
    public void publishToLines(ArrayList<FB2Line> lines, LineCreationParams params) {
        params.insertSpace = false;
    }

}
