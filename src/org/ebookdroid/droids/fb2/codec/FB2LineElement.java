package org.ebookdroid.droids.fb2.codec;

import android.graphics.Canvas;

public interface FB2LineElement extends FB2MarkupElement {

    float render(Canvas c, int y, int x, float additionalWidth, float left, float right);
}
