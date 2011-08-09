package org.ebookdroid.core.curl;

import android.graphics.Canvas;
import android.view.MotionEvent;


public interface PageAnimator {

    void init();

    void resetPageIndexes();

    boolean onTouchEvent(MotionEvent event);

    void onDraw(Canvas canvas);

    void setViewDrawn(boolean b);

    void FlipAnimationStep();

}
