package org.emdev.common.textmarkup;

import org.emdev.common.textmarkup.line.LineStream;


public interface MarkupElement {

    void publishToLines(LineStream lines);

}
