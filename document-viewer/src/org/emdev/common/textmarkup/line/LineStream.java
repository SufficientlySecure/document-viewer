package org.emdev.common.textmarkup.line;

import org.ebookdroid.droids.fb2.codec.LineCreationParams;
import org.ebookdroid.droids.fb2.codec.ParsedContent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.emdev.common.textmarkup.JustificationMode;

public class LineStream implements Collection<Line> {

    public final LineCreationParams params;

    private final ArrayList<Line> lines = new ArrayList<Line>();

    public LineStream(final ParsedContent content, final int maxLineWidth, final JustificationMode jm,
            final boolean hyphenEnabled) {
        this.params = new LineCreationParams(content, maxLineWidth, jm, hyphenEnabled);
    }

    public LineStream(final LineCreationParams params) {
        this.params = params;
    }

    public Line add() {
        final Line line = new Line(params.content, params.maxLineWidth, params.jm);
        lines.add(line);

        if (params.extraSpace > 0) {
            line.append(new LineFixedWhiteSpace(params.extraSpace, 0));
        }

        return line;
    }

    public Line add(final int maxLineWidth, final JustificationMode jm) {
        final Line line = new Line(params.content, maxLineWidth, jm);
        lines.add(line);
        return line;
    }

    public Line last() {
        return lines.isEmpty() ? null : lines.get(lines.size() - 1);
    }

    public Line tail() {
        final Line last = last();
        if (last == null || last.committed) {
            return add();
        }
        return last;
    }

    public void commitParagraph() {
        if (lines.isEmpty()) {
            return;
        }
        final Line last = lines.get(lines.size() - 1);
        final JustificationMode lastJm = params.jm == JustificationMode.Justify ? JustificationMode.Left : params.jm;
        for (int i = lines.size() - 1; i >= 0; i--) {
            final Line l = lines.get(i);
            if (l.committed) {
                break;
            }
            l.applyJustification(l != last ? params.jm : lastJm);
        }
    }

    public Line get(final int i) {
        return lines.get(i);
    }

    @Override
    public boolean add(final Line object) {
        return lines.add(object);
    }

    @Override
    public boolean containsAll(final Collection<?> collection) {
        return lines.containsAll(collection);
    }

    @Override
    public boolean addAll(final Collection<? extends Line> collection) {
        return lines.addAll(collection);
    }

    @Override
    public void clear() {
        lines.clear();
    }

    @Override
    public boolean removeAll(final Collection<?> collection) {
        return lines.removeAll(collection);
    }

    @Override
    public int size() {
        return lines.size();
    }

    @Override
    public boolean isEmpty() {
        return lines.isEmpty();
    }

    @Override
    public boolean contains(final Object object) {
        return lines.contains(object);
    }

    @Override
    public boolean retainAll(final Collection<?> collection) {
        return lines.retainAll(collection);
    }

    @Override
    public boolean remove(final Object object) {
        return lines.remove(object);
    }

    @Override
    public Object[] toArray() {
        return lines.toArray();
    }

    @Override
    public <T> T[] toArray(final T[] contents) {
        return lines.toArray(contents);
    }

    @Override
    public Iterator<Line> iterator() {
        return lines.iterator();
    }

}
