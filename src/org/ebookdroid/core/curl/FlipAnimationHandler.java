package org.ebookdroid.core.curl;

import android.os.Handler;
import android.os.Message;

/**
 * Inner class used to make a fixed timed animation of the curl effect.
 */
class FlipAnimationHandler extends Handler {

    /**
     * 
     */
    private final PageAnimator animator;

    /**
     * @param singlePageCurler
     */
    FlipAnimationHandler(PageAnimator singlePageCurler) {
        this.animator = singlePageCurler;
    }

    @Override
    public void handleMessage(final Message msg) {
        this.animator.FlipAnimationStep();
    }

    public void sleep(final long millis) {
        this.removeMessages(0);
        sendMessageDelayed(obtainMessage(0), millis);
    }
}