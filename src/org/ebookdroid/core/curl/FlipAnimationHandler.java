package org.ebookdroid.core.curl;

import android.os.Handler;
import android.os.Message;

/**
 * Inner class used to make a fixed timed animation of the curl effect.
 */
class FlipAnimationHandler extends Handler {

    private final PageAnimator animator;

    FlipAnimationHandler(PageAnimator singlePageCurler) {
        this.animator = singlePageCurler;
    }

    /**
     * {@inheritDoc}
     *
     * @see android.os.Handler#handleMessage(android.os.Message)
     */
    @Override
    public void handleMessage(final Message msg) {
        this.animator.flipAnimationStep();
    }

    public void sleep(final long millis) {
        this.removeMessages(0);
        sendMessageDelayed(obtainMessage(0), millis);
    }
}