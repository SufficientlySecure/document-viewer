package org.ebookdroid.droids.fb2.codec;

import org.ebookdroid.droids.fb2.codec.FB2Document.LineCreationParams;

import java.util.ArrayList;
import java.util.List;

import org.emdev.utils.LengthUtils;

public class FB2MarkupNote implements FB2MarkupElement {

    private final String ref;

    public FB2MarkupNote(final String ref) {
        this.ref = ref;
    }

    public void publishToDocument(final FB2Document doc) {
    }

    @Override
    public void publishToLines(ArrayList<FB2Line> lines, LineCreationParams params) {
        final List<FB2Line> note = params.doc.getNote(ref);
        if (note != null && LengthUtils.isNotEmpty(lines)) {
            final FB2Line line = FB2Line.getLastLine(lines, params.maxLineWidth, params.jm);
            line.addNote(note);
        }
    }

}
