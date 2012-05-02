package org.emdev.utils.textmarkup;

import org.ebookdroid.droids.fb2.codec.LineCreationParams;

import java.util.ArrayList;

import org.emdev.utils.textmarkup.line.Line;

public interface MarkupElement {
    void publishToLines(ArrayList<Line> lines, LineCreationParams params);
}
