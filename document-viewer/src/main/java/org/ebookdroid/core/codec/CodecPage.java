package org.ebookdroid.core.codec;

import org.ebookdroid.common.bitmaps.ByteBufferBitmap;
import org.ebookdroid.core.ViewState;

import android.graphics.RectF;

import java.util.List;

public interface CodecPage {

    int getWidth();

    int getHeight();

    ByteBufferBitmap renderBitmap(ViewState viewState, int width, int height, RectF pageSliceBounds);

    List<PageLink> getPageLinks();

    List<PageTextBox> getPageText();

    List<? extends RectF> searchText(final String pattern);

    void recycle();

    boolean isRecycled();
}
