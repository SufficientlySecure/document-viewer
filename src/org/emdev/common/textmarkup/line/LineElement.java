package org.emdev.common.textmarkup.line;

import android.graphics.Canvas;

import org.emdev.common.textmarkup.MarkupElement;


public interface LineElement extends MarkupElement {

    float render(Canvas c, int y, int x, float additionalWidth, float left, float right, final int nightmode);
}
