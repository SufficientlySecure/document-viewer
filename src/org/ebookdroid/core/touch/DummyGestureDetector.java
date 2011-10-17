package org.ebookdroid.core.touch;

import android.view.MotionEvent;


public class DummyGestureDetector implements IMultiTouchZoom {

    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }

}
