package org.emdev.utils.textmarkup;


import org.ebookdroid.droids.fb2.codec.LineCreationParams;

import java.util.ArrayList;

import org.emdev.utils.LengthUtils;
import org.emdev.utils.textmarkup.line.Line;

public class MarkupParagraphEnd implements MarkupElement {

    public static final MarkupParagraphEnd E = new MarkupParagraphEnd();

    private MarkupParagraphEnd() {
    }

    @Override
    public void publishToLines(ArrayList<Line> lines, LineCreationParams params) {
        if (LengthUtils.isEmpty(lines)) {
            return;
        }
        final Line last = lines.get(lines.size() - 1);
        final JustificationMode lastJm = params.jm == JustificationMode.Justify ? JustificationMode.Left : params.jm;
        for (int i = lines.size() - 1; i >= 0; i--) {
            Line l = lines.get(i);
            if (l.committed) {
                break;
            }
            l.applyJustification(l != last ? params.jm : lastJm);
        }
    }

}
