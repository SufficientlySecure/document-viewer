package org.emdev.common.textmarkup;


import org.ebookdroid.droids.fb2.codec.FB2Document;
import org.ebookdroid.droids.fb2.codec.LineCreationParams;

import java.util.ArrayList;
import java.util.List;

import org.emdev.common.textmarkup.line.Line;
import org.emdev.utils.LengthUtils;

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
            final Line line = Line.getLastLine(lines, params);
            line.addNote(note);
        }
    }

}
