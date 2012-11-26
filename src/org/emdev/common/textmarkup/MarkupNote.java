package org.emdev.common.textmarkup;

import org.emdev.common.textmarkup.line.Line;
import org.emdev.common.textmarkup.line.LineStream;
import org.emdev.utils.LengthUtils;

public class MarkupNote implements MarkupElement {

    private final String ref;

    public MarkupNote(final String ref) {
        this.ref = ref;
    }

    @Override
    public void publishToLines(final LineStream lines) {
        final LineStream note = lines.params.content.getNote(ref, lines.params.hyphenEnabled);
        if (note != null && LengthUtils.isNotEmpty(lines)) {
            final Line line = lines.tail();
            line.addNote(note, false);
        }
    }

}
