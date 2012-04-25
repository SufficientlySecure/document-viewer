package org.ebookdroid.droids.fb2.codec;

import org.ebookdroid.droids.fb2.codec.FB2Document.LineCreationParams;

import java.util.ArrayList;

public class FB2MarkupNewParagraph implements FB2MarkupElement {

    private final FB2LineFixedWhiteSpace offset;

    public FB2MarkupNewParagraph(final FB2LineFixedWhiteSpace offset) {
        this.offset = offset;
    }

    @Override
    public void publishToLines(ArrayList<FB2Line> lines, LineCreationParams params) {
        if (params.jm != JustificationMode.Center && offset != null) {
            offset.publishToLines(lines, params);
        }
    }

}
