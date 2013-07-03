package org.emdev.common.textmarkup;

import org.ebookdroid.droids.fb2.codec.LineCreationParams;

import java.util.ArrayList;
import java.util.UUID;

import org.emdev.common.textmarkup.line.Line;
import org.emdev.common.textmarkup.line.LineStream;
import org.emdev.common.textmarkup.line.MultiLineElement;

public class MarkupTable implements MarkupElement {

    private static final int DOUBLE_BORDER_WIDTH = MultiLineElement.BORDER_WIDTH * 2;

    public final String uuid = UUID.randomUUID().toString();

    final ArrayList<ArrayList<Cell>> rows = new ArrayList<ArrayList<Cell>>();
    int maxColCount;

    public void addRow() {
        rows.add(new ArrayList<Cell>());
    }

    public Cell addCol() {
        if (rows.isEmpty()) {
            addRow();
        }

        final Cell c = new Cell();
        final ArrayList<Cell> row = rows.get(rows.size() - 1);
        row.add(c);

        c.stream = uuid + ":" + rows.size() + ":" + row.size();

        maxColCount = Math.max(maxColCount, row.size());

        return c;
    }

    @Override
    public void publishToLines(final LineStream lines) {
        if (rows.isEmpty()) {
            return;
        }

        final int cellWidth = lines.params.maxLineWidth / maxColCount;

        for (final ArrayList<Cell> row : rows) {
            final Line rowLine = lines.add();
            rowLine.applyJustification(JustificationMode.Center);

            int maxHeight = 0;
            for (final Cell cell : row) {
                final int cellHeight = MultiLineElement.calcHeight(cell.getLines(lines.params, cellWidth));
                maxHeight = Math.max(maxHeight, cellHeight);
            }

            for (final Cell cell : row) {
                final MultiLineElement cellElem = cell.getElement(cellWidth, maxHeight);
                rowLine.append(cellElem);
                cellElem.applyNotes(rowLine);
            }
        }
    }

    public class Cell {

        public JustificationMode align = JustificationMode.Left;
        public String stream;
        public boolean hasBackground;
        private LineStream lines;

        private LineStream getLines(final LineCreationParams params, final int cellWidth) {
            if (lines == null) {
                lines = params.content.getStreamLines(stream, cellWidth - DOUBLE_BORDER_WIDTH, align, params.hyphenEnabled);
            }
            return lines;
        }

        private MultiLineElement getElement(final int cellWidth, final int maxHeight) {
            return new MultiLineElement(cellWidth, maxHeight + DOUBLE_BORDER_WIDTH, lines, true, hasBackground);
        }
    }
}
