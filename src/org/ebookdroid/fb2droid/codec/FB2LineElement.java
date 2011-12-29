package org.ebookdroid.fb2droid.codec;

import android.graphics.Canvas;

public interface FB2LineElement extends FB2MarkupElement {

    float render(Canvas c, int y, int x, float additionalWidth);
}
