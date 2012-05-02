package org.emdev.utils.textmarkup.line;

import org.ebookdroid.droids.fb2.codec.FB2Page;

import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.List;

import org.emdev.utils.LengthUtils;
import org.emdev.utils.textmarkup.FontStyle;
import org.emdev.utils.textmarkup.MarkupTitle;
import org.emdev.utils.textmarkup.JustificationMode;

public class Line {

    public final ArrayList<AbstractLineElement> elements = new ArrayList<AbstractLineElement>();
    private int height;
    float width = 0;
    private boolean hasNonWhiteSpaces = false;
    private List<Line> footnotes;
    public boolean committed;
    private int sizeableCount;
    public float spaceWidth;
    private boolean justified;
    private JustificationMode justification = JustificationMode.Justify;
    private MarkupTitle title;
    private int maxLineWidth;

    public Line(int lineWidth, JustificationMode jm) {
        this.maxLineWidth = lineWidth;
        justification = jm;
    }

    public Line append(final AbstractLineElement element) {
        elements.add(element);
        if (element.height > height) {
            height = element.height;
        }
        if (element instanceof LineFixedWhiteSpace) {
            // Do nothing
        } else if (element instanceof LineWhiteSpace) {
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
            final Line line = footnotes.get(i);
            h += line.getHeight();
        }
        return h;
    }

    public void render(final Canvas c, final int x, final int y, float left, float right) {
        ensureJustification();
        float x1 = x;
        for (int i = 0, n = elements.size(); i < n; i++) {
            final AbstractLineElement e = elements.get(i);
            x1 += e.render(c, y, (int) x1, spaceWidth, left, right);
        }
    }

    public static Line getLastLine(final ArrayList<Line> lines, int maxLineWidth, JustificationMode jm) {
        if (lines.size() == 0) {
            lines.add(new Line(maxLineWidth, jm));
        }
        Line fb2Line = lines.get(lines.size() - 1);
        if (fb2Line.committed) {
            fb2Line = new Line(maxLineWidth, jm);
            lines.add(fb2Line);
        }
        return fb2Line;
    }

    public void ensureJustification() {
        if (!justified) {
            switch (justification) {
                case Center:
                    final float x = (maxLineWidth - (width)) / 2;
                    elements.add(0, new LineFixedWhiteSpace(x, height));
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
                    elements.add(0, new LineFixedWhiteSpace(x1, height));
                    break;
            }
            justified = true;
        }
    }

    public boolean hasNonWhiteSpaces() {
        return hasNonWhiteSpaces;
    }

    public List<Line> getFootNotes() {
        return footnotes;
    }

    public void addNote(final List<Line> noteLines) {
        if (noteLines == null) {
            return;
        }
        if (footnotes == null) {
            footnotes = new ArrayList<Line>();
            final Line lastLine = new Line(FB2Page.PAGE_WIDTH / 4, justification);
            footnotes.add(lastLine);
            lastLine.append(new HorizontalRule(FB2Page.PAGE_WIDTH / 4, FontStyle.FOOTNOTE.getFontSize()));
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

    public void setTitle(MarkupTitle fb2MarkupTitle) {
        this.title = fb2MarkupTitle;
    }

    public MarkupTitle getTitle() {
        return this.title;
    }


}
