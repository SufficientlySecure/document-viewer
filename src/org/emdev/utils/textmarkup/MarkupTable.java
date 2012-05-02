package org.emdev.utils.textmarkup;


import org.ebookdroid.droids.fb2.codec.LineCreationParams;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.emdev.utils.LengthUtils;
import org.emdev.utils.textmarkup.line.Line;
import org.emdev.utils.textmarkup.line.MultiLineElement;

public class MarkupTable implements MarkupElement {


    public class Cell {
        public JustificationMode align = JustificationMode.Left;
        public String stream;
        public boolean hasBackground;
    }

    private static final int DOUBLE_BORDER_WIDTH = MultiLineElement.BORDER_WIDTH * 2;

    public final String uuid = UUID.randomUUID().toString();

    final ArrayList<ArrayList<Cell>> rows = new ArrayList<ArrayList<Cell>>();

    @Override
    public void publishToLines(ArrayList<Line> lines, LineCreationParams params) {
        if (getRowCount() > 0) {
            final int cellWidth = params.maxLineWidth / getMaxColCount();
            for (int i = 0, n = rows.size(); i < n; i++) {
                final int colCount = getColCount(i);
                ArrayList<List<Line>> cells = new ArrayList<List<Line>>(colCount);
                Line row = new Line(params.maxLineWidth, params.jm);
                cells.clear();
                int maxHeight = 0;
                for (int j = 0; j < colCount; j++) {
                    final Cell cell = rows.get(i).get(j);
                    final List<Line> cellLines = params.content.getStreamLines(cell.stream,
                            cellWidth - DOUBLE_BORDER_WIDTH, cell.align);
                    cells.add(cellLines);
                    final int height = MultiLineElement.calcHeight(cellLines);
                    if (height > maxHeight) {
                        maxHeight = height;
                    }
                }
                for (int j = 0; j < colCount; j++) {
                    List<Line> list = cells.get(j);
                    final MultiLineElement cell = new MultiLineElement(cellWidth, maxHeight + DOUBLE_BORDER_WIDTH, list, true, rows.get(i).get(j).hasBackground);
                    row.append(cell);
                    cell.applyNotes(row);
                }

                row.applyJustification(JustificationMode.Center);
                lines.add(row);
            }
        }
    }

    private int getMaxColCount() {
        int max = 0;
        if (LengthUtils.isNotEmpty(rows)) {
            for (int i = 0, n = rows.size(); i < n; i++) {
                final int size = rows.get(i).size();
                if (size > max) {
                    max = size;
                }
            }
        }
        return max;
    }

    public void addRow() {
        rows.add(new ArrayList<Cell>());
    }

    public void addCol(Cell c) {
        if (LengthUtils.isNotEmpty(rows)) {
            rows.get(rows.size() - 1).add(c);
        }
    }

    public int getRowCount() {
        return rows.size();
    }

    public int getColCount(int row) {
        if (row >= 0 && row < rows.size()) {
            return rows.get(row).size();
        }
        return 0;
    }

}
