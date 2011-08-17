package org.ebookdroid.core.curl;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;


public interface PageAnimator {

    PageAnimationType getType();

    void init();

    void resetPageIndexes();

    boolean handleTouchEvent(MotionEvent event);

    void draw(Canvas canvas, RectF viewRect);

    void setViewDrawn(boolean b);

    void FlipAnimationStep();
}
