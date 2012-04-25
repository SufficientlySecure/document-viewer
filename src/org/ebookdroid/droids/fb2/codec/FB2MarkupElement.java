package org.ebookdroid.droids.fb2.codec;

import org.ebookdroid.droids.fb2.codec.FB2Document.LineCreationParams;

import java.util.ArrayList;

public interface FB2MarkupElement {
    void publishToLines(ArrayList<FB2Line> lines, LineCreationParams params);
}
