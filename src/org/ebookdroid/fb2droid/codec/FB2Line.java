package org.ebookdroid.fb2droid.codec;

import org.ebookdroid.utils.LengthUtils;

import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.List;

public class FB2Line {

    private final ArrayList<AbstractFB2LineElement> elements = new ArrayList<AbstractFB2LineElement>();
    private int height;
    float width = 0;
    private boolean hasNonWhiteSpaces = false;
    private List<FB2Line> footnotes;
    private boolean committed;
    private int sizeableCount;
    private float spaceWidth;
    private boolean justified;
    private JustificationMode justification = JustificationMode.Justify;

    public FB2Line() {
    }

    public FB2Line append(final AbstractFB2LineElement element) {
        elements.add(element);
        if (element.height > height) {
            height = element.height;
        }
        if (element instanceof FB2LineFixedWhiteSpace) {
            // Do nothing
        } else if (element instanceof FB2LineWhiteSpace) {
            sizeableCount++;
        } else {
            hasNonWhiteSpaces = true;
        }
        width += element.width;
        return this;
    }

    public int getTotalHeight() {
        int h = height;
        for (int i = 0, n = Math.min(2, LengthUtils.length(footnotes)); i < n; i++) {
            final FB2Line line = footnotes.get(i);
            h += line.getHeight();
        }
        return h;
    }

    public void render(final Canvas c, final int y) {
        applyJustification();
        float x = FB2Page.MARGIN_X;
        for (final AbstractFB2LineElement e : elements) {
            x += e.render(c, y, (int) x, spaceWidth);
        }
    }

    public static FB2Line getLastLine(final ArrayList<FB2Line> lines) {
        if (lines.size() == 0) {
            lines.add(new FB2Line());
        }
        FB2Line fb2Line = lines.get(lines.size() - 1);
        if (fb2Line.committed) {
            fb2Line = new FB2Line();
            lines.add(fb2Line);
        }
        return fb2Line;
    }

    private void applyJustification() {
        if (!justified) {
            switch (justification) {
                case Center:
                    final float x = (FB2Page.PAGE_WIDTH - (width + 2 * FB2Page.MARGIN_X)) / 2;
                    elements.add(0, new FB2LineFixedWhiteSpace(x, height));
                    break;
                case Left:
                    break;
                case Justify:
                    if (sizeableCount > 0) {
                        spaceWidth = (FB2Page.PAGE_WIDTH - (width + 2 * FB2Page.MARGIN_X)) / sizeableCount;
                    } else {
                        spaceWidth = 0;
                    }
                    break;
                case Right:
                    final float x1 = (FB2Page.PAGE_WIDTH - (width + 2 * FB2Page.MARGIN_X));
                    elements.add(0, new FB2LineFixedWhiteSpace(x1, height));
                    break;
            }
            justified = true;
        }
    }

    public boolean hasNonWhiteSpaces() {
        return hasNonWhiteSpaces;
    }

    public List<FB2Line> getFootNotes() {
        return footnotes;
    }

    public void addNote(final List<FB2Line> noteLines) {
        if (noteLines == null) {
            return;
        }
        if (footnotes == null) {
            footnotes = new ArrayList<FB2Line>();
            final FB2Line lastLine = new FB2Line();
            footnotes.add(lastLine);
            lastLine.append(new FB2HorizontalRule(FB2Page.PAGE_WIDTH / 4, RenderingStyle.FOOTNOTE_SIZE));
            lastLine.applyJustification(JustificationMode.Left);
        }
        footnotes.addAll(noteLines);
    }

    public void applyJustification(final JustificationMode jm) {
        if (committed) {
            return;
        }
        justification = jm;
        committed = true;
    }

    public int getHeight() {
        return height;
    }
}
