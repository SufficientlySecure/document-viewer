package org.ebookdroid.droids.fb2.codec;

import org.ebookdroid.droids.fb2.codec.FB2Document.LineCreationParams;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FB2MarkupTable implements FB2MarkupElement {

    final String uuid = UUID.randomUUID().toString();

    int rowCount = 0;
    int colCount = 0;

    @Override
    public void publishToLines(ArrayList<FB2Line> lines, LineCreationParams params) {
        if (colCount > 0 && rowCount > 0) {
            final int cellWidth = params.maxLineWidth / colCount;
            ArrayList<List<FB2Line>> cells = new ArrayList<List<FB2Line>>(colCount);
            for (int i = 0; i < rowCount; i++) {
                FB2Line row = new FB2Line(params.maxLineWidth);
                cells.clear();
                int maxHeight = 0;
                for (int j = 0; j < colCount; j++) {
                    final List<FB2Line> cellLines = params.doc.getStreamLines(uuid + ":" + (i + 1) + ":" + (j + 1),
                            cellWidth - 20, JustificationMode.Left);
                    cells.add(cellLines);
                    final int height = FB2MultiLineElement.calcHeight(cellLines);
                    if (height > maxHeight) {
                        maxHeight = height;
                    }
                }
                for (List<FB2Line> list : cells) {
                    row.append(new FB2MultiLineElement(cellWidth, maxHeight + 20, list, true, false));
                }

                row.applyJustification(JustificationMode.Center);
                lines.add(row);
            }
        }
    }

}
