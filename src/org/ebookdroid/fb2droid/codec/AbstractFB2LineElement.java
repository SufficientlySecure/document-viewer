package org.ebookdroid.fb2droid.codec;

import android.graphics.RectF;

public abstract class AbstractFB2LineElement implements FB2LineElement {

    public final int height;
    public float width;
    public final boolean sizeable;

    public AbstractFB2LineElement(final float width, final int height, final boolean sizeable) {
        this.height = height;
        this.width = width;
        this.sizeable = sizeable;
    }

    public AbstractFB2LineElement(final RectF rect) {
        this(rect.width(), (int) rect.height(), false);
    }

    @Override
    public void publishToDocument(final FB2Document doc) {
        doc.publishElement(this);
    }
}
