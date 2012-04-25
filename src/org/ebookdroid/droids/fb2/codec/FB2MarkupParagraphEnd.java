package org.ebookdroid.droids.fb2.codec;

import org.ebookdroid.droids.fb2.codec.FB2Document.LineCreationParams;

import java.util.ArrayList;

import org.emdev.utils.LengthUtils;

public class FB2MarkupParagraphEnd implements FB2MarkupElement {

    public static final FB2MarkupParagraphEnd E = new FB2MarkupParagraphEnd();

    private FB2MarkupParagraphEnd() {
    }

    @Override
    public void publishToLines(ArrayList<FB2Line> lines, LineCreationParams params) {
        if (LengthUtils.isEmpty(lines)) {
            return;
        }
        final FB2Line last = lines.get(lines.size() - 1);
        final JustificationMode lastJm = params.jm == JustificationMode.Justify ? JustificationMode.Left : params.jm;
        for (int i = lines.size() - 1; i >= 0; i--) {
            FB2Line l = lines.get(i);
            if (l.committed) {
                break;
            }
            l.applyJustification(l != last ? params.jm : lastJm);
        }
    }

}
