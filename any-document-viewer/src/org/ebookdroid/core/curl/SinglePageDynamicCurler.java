package org.ebookdroid.core.curl;

import org.ebookdroid.core.SinglePageController;

/**
 * The Class SinglePageCurler.
 * 
 * Used for drawing page curl animation
 * 
 * @author Moritz 'Moss' Wundke (b.thax.dcg@gmail.com)
 * 
 */
public class SinglePageDynamicCurler extends AbstractSinglePageCurler {

    public SinglePageDynamicCurler(final SinglePageController singlePageDocumentView) {
        super(PageAnimationType.CURLER_DYNAMIC, singlePageDocumentView);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#getInitialXForBackFlip(int)
     */
    @Override
    protected int getInitialXForBackFlip(final int width) {
        return width << 1;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#updateValues()
     */
    @Override
    protected void updateValues() {
        final int width = view.getWidth();
        final int height = view.getHeight();

        // F will follow the finger, we add a small displacement
        // So that we can see the edge
        mF.x = width - mMovement.x + 0.1f;
        mF.y = height - mMovement.y + 0.1f;

        // Set min points
        if (mA.x == 0) {
            mF.x = Math.min(mF.x, mOldF.x);
            mF.y = Math.max(mF.y, mOldF.y);
        }

        // Get diffs
        final float deltaX = width - mF.x;
        final float deltaY = height - mF.y;

        final float delta_sq = deltaX * deltaX + deltaY * deltaY;
        final float tangA = deltaY / deltaX;

        mA.x = width - (delta_sq / (2 * deltaX));
        mA.y = height;

        mD.y = height - (delta_sq / (2 * deltaY));
        mD.x = width;

        mA.x = Math.max(0, mA.x);
        if (mA.x == 0) {
            mOldF.x = mF.x;
            mOldF.y = mF.y;
        }

        // Get W
        mE.x = mD.x;
        mE.y = mD.y;

        // Correct
        if (mD.y < 0) {
            mD.x = width + tangA * mD.y;
            mE.y = 0;
            mE.x = width + 2 * mD.y * tangA / (1 - tangA * tangA);
        }
    }

}
