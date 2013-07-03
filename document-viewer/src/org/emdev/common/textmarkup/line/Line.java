package org.emdev.common.textmarkup.line;

import org.ebookdroid.droids.fb2.codec.FB2Page;
import org.ebookdroid.droids.fb2.codec.ParsedContent;

import android.graphics.Canvas;

import java.util.ArrayList;

import org.emdev.common.textmarkup.JustificationMode;
import org.emdev.common.textmarkup.MarkupTitle;
import org.emdev.common.textmarkup.TextStyle;
import org.emdev.utils.LengthUtils;

public class Line {

    public final ParsedContent content;
    public final ArrayList<AbstractLineElement> elements = new ArrayList<AbstractLineElement>();

    private int height;
    float width = 0;
    private boolean hasNonWhiteSpaces = false;
    private LineStream footnotes;
    public boolean committed;
    private int sizeableCount;
    public float spaceWidth;
    private boolean justified;
    private JustificationMode justification = JustificationMode.Justify;
    private MarkupTitle title;
    private final int maxLineWidth;
    private volatile boolean recycled;

    public Line(final ParsedContent content, final int lineWidth, final JustificationMode jm) {
        this.content = content;
        this.maxLineWidth = lineWidth;
        justification = jm;
    }

    public void recycle() {
        recycled = true;
        elements.clear();
        if (footnotes != null) {
            for (final Line l : footnotes) {
                l.recycle();
            }
            footnotes.clear();
            footnotes = null;
        }
    }

    public Line append(final AbstractLineElement element) {
        if (LengthUtils.isEmpty(elements) && element instanceof LineWhiteSpace) {
            return this;
        }
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
            h += line.height;
        }
        return h;
    }

    public void render(final Canvas c, final int x, final int y, final float left, final float right,
            final int nightmode) {
        ensureJustification();
        float x1 = x;
        for (int i = 0, n = elements.size(); i < n && !recycled; i++) {
            final AbstractLineElement e = elements.get(i);
            x1 += e.render(c, y, (int) x1, spaceWidth, left, right, nightmode);
        }
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

    public LineStream getFootNotes() {
        return footnotes;
    }

    public void addNote(final LineStream noteLines, boolean skipFirst) {
        if (noteLines == null) {
            return;
        }
        if (footnotes == null) {
            footnotes = new LineStream(noteLines.params);
            final Line lastLine = new Line(content, FB2Page.PAGE_WIDTH / 4, justification);
            footnotes.add(lastLine);
            lastLine.append(new HorizontalRule(FB2Page.PAGE_WIDTH / 4, TextStyle.FOOTNOTE.getFontSize()));
            lastLine.applyJustification(JustificationMode.Left);
        }
        if (skipFirst) {
            for (final Line l : noteLines) {
                if (skipFirst) {
                    skipFirst = false;
                } else {
                    footnotes.add(l);
                }
            }
        } else {
            footnotes.addAll(noteLines);
        }
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

    public void setTitle(final MarkupTitle fb2MarkupTitle) {
        this.title = fb2MarkupTitle;
    }

    public MarkupTitle getTitle() {
        return this.title;
    }

    public boolean isEmpty() {
        if (LengthUtils.isEmpty(elements)) {
            return true;
        }
        for (final AbstractLineElement e : elements) {
            if (e instanceof LineFixedWhiteSpace || e instanceof LineWhiteSpace) {
                continue;
            }
            return false;
        }
        return true;
    }

}
