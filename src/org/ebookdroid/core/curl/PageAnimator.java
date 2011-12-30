package org.ebookdroid.core.curl;

import org.ebookdroid.core.Page;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.core.touch.IGestureDetector;

import android.graphics.Canvas;

public interface PageAnimator extends IGestureDetector {

    PageAnimationType getType();

    void init();

    void resetPageIndexes(final int currentIndex);

    void draw(Canvas canvas, final ViewState viewState);

    void setViewDrawn(boolean b);

    void FlipAnimationStep();

    boolean isPageVisible(final Page page, final ViewState viewState);

    void pageUpdated(int viewIndex);

    void animate(int direction);
}
