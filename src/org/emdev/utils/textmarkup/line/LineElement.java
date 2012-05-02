package org.emdev.utils.textmarkup.line;

import android.graphics.Canvas;

import org.emdev.utils.textmarkup.MarkupElement;


public interface LineElement extends MarkupElement {

    float render(Canvas c, int y, int x, float additionalWidth, float left, float right);
}
