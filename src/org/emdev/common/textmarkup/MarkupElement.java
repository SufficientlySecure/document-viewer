package org.emdev.common.textmarkup;

import org.ebookdroid.droids.fb2.codec.LineCreationParams;

import java.util.ArrayList;

import org.emdev.common.textmarkup.line.Line;

public interface MarkupElement {
    void publishToLines(ArrayList<Line> lines, LineCreationParams params);
}
