package org.emdev.utils.textmarkup;


import org.ebookdroid.droids.fb2.codec.FB2Document;
import org.ebookdroid.droids.fb2.codec.LineCreationParams;

import java.util.ArrayList;
import java.util.List;

import org.emdev.utils.LengthUtils;
import org.emdev.utils.textmarkup.line.Line;

public class MarkupNote implements MarkupElement {

    private final String ref;

    public MarkupNote(final String ref) {
        this.ref = ref;
    }

    public void publishToDocument(final FB2Document doc) {
    }

    @Override
    public void publishToLines(ArrayList<Line> lines, LineCreationParams params) {
        final List<Line> note = params.content.getNote(ref);
        if (note != null && LengthUtils.isNotEmpty(lines)) {
            final Line line = Line.getLastLine(lines, params.maxLineWidth, params.jm);
            line.addNote(note);
        }
    }

}
