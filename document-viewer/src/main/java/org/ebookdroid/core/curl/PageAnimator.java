package org.ebookdroid.core.curl;

import org.ebookdroid.common.touch.IGestureDetector;
import org.ebookdroid.core.EventGLDraw;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.ViewState;

public interface PageAnimator extends IGestureDetector {

    PageAnimationType getType();

    void init();

    void resetPageIndexes(final int currentIndex);

    void draw(EventGLDraw event);

    void setViewDrawn(boolean b);

    void flipAnimationStep();

    boolean isPageVisible(final Page page, final ViewState viewState);

    void pageUpdated(ViewState viewState, Page page);

    void animate(int direction);
}
