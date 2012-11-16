package org.ebookdroid.common.touch;

public class MultiTouchGestureDetectorFactory {

    public static IGestureDetector create(final IMultiTouchListener mtListener) {
        return new MultiTouchGestureDetector(mtListener);
    }
}
