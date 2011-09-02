package org.ebookdroid.core.curl;

import org.ebookdroid.core.ViewState;

import android.graphics.Canvas;
import android.view.MotionEvent;

public interface PageAnimator {

    PageAnimationType getType();

    void init();

    void resetPageIndexes(final int currentIndex);

    boolean handleTouchEvent(MotionEvent event);

    void draw(Canvas canvas, final ViewState viewState);

    void setViewDrawn(boolean b);

    void FlipAnimationStep();

    int getBackIndex();

    int getForeIndex();
}
