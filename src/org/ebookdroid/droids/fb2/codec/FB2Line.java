package org.ebookdroid.droids.fb2.codec;

import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.List;

import org.emdev.utils.LengthUtils;

public class FB2Line {

    final ArrayList<AbstractFB2LineElement> elements = new ArrayList<AbstractFB2LineElement>();
    private int height;
    float width = 0;
    private boolean hasNonWhiteSpaces = false;
    private List<FB2Line> footnotes;
    boolean committed;
    private int sizeableCount;
    float spaceWidth;
    private boolean justified;
    private JustificationMode justification = JustificationMode.Justify;
    private FB2MarkupTitle title;
    private int maxLineWidth;

    public FB2Line(int lineWidth) {
        this.maxLineWidth = lineWidth;
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

    public void render(final Canvas c, final int y, float left, float right) {
        ensureJustification();
        float x = FB2Page.MARGIN_X;
        for (int i = 0, n = elements.size(); i < n; i++) {
            final AbstractFB2LineElement e = elements.get(i);
            x += e.render(c, y, (int) x, spaceWidth, left, right);
        }
    }

    public static FB2Line getLastLine(final ArrayList<FB2Line> lines, int maxLineWidth) {
        if (lines.size() == 0) {
            lines.add(new FB2Line(maxLineWidth));
        }
        FB2Line fb2Line = lines.get(lines.size() - 1);
        if (fb2Line.committed) {
            fb2Line = new FB2Line(maxLineWidth);
            lines.add(fb2Line);
        }
        return fb2Line;
    }

    public void ensureJustification() {
        if (!justified) {
            switch (justification) {
                case Center:
                    final float x = (maxLineWidth - (width)) / 2;
                    elements.add(0, new FB2LineFixedWhiteSpace(x, height));
                    break;
                case Left:
                    break;
                case Justify:
                    if (sizeableCount > 0) {
                        spaceWidth = (maxLineWidth - (width)) / sizeableCount;
                    } else {
                        spaceWidth = 0;
                    }
                    break;
                case Right:
                    final float x1 = (maxLineWidth - (width));
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
            final FB2Line lastLine = new FB2Line(FB2Page.PAGE_WIDTH / 4);
            footnotes.add(lastLine);
            lastLine.append(new FB2HorizontalRule(FB2Page.PAGE_WIDTH / 4, FB2FontStyle.FOOTNOTE.getFontSize()));
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

    public boolean appendable() {
        return true;
    }

    public void setTitle(FB2MarkupTitle fb2MarkupTitle) {
        this.title = fb2MarkupTitle;
    }

    public FB2MarkupTitle getTitle() {
        return this.title;
    }


}
