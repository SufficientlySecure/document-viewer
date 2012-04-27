package org.ebookdroid.droids.fb2.codec;

import org.ebookdroid.droids.fb2.codec.FB2Document.LineCreationParams;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.emdev.utils.LengthUtils;

public class FB2MarkupTable implements FB2MarkupElement {


    public class Cell {
        JustificationMode align = JustificationMode.Left;
        String stream;
        boolean hasBackground;
    }

    private static final int DOUBLE_BORDER_WIDTH = FB2MultiLineElement.BORDER_WIDTH * 2;

    final String uuid = UUID.randomUUID().toString();

    final ArrayList<ArrayList<Cell>> rows = new ArrayList<ArrayList<Cell>>();

    @Override
    public void publishToLines(ArrayList<FB2Line> lines, LineCreationParams params) {
        if (getRowCount() > 0) {
            final int cellWidth = params.maxLineWidth / getMaxColCount();
            for (int i = 0, n = rows.size(); i < n; i++) {
                final int colCount = getColCount(i);
                ArrayList<List<FB2Line>> cells = new ArrayList<List<FB2Line>>(colCount);
                FB2Line row = new FB2Line(params.maxLineWidth, params.jm);
                cells.clear();
                int maxHeight = 0;
                for (int j = 0; j < colCount; j++) {
                    final Cell cell = rows.get(i).get(j);
                    final List<FB2Line> cellLines = params.doc.getStreamLines(cell.stream,
                            cellWidth - DOUBLE_BORDER_WIDTH, cell.align);
                    cells.add(cellLines);
                    final int height = FB2MultiLineElement.calcHeight(cellLines);
                    if (height > maxHeight) {
                        maxHeight = height;
                    }
                }
                for (int j = 0; j < colCount; j++) {
                    List<FB2Line> list = cells.get(j);
                    final FB2MultiLineElement cell = new FB2MultiLineElement(cellWidth, maxHeight + DOUBLE_BORDER_WIDTH, list, true, rows.get(i).get(j).hasBackground);
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
