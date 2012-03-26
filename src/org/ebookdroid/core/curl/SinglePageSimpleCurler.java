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
public class SinglePageSimpleCurler extends AbstractSinglePageCurler {

    public SinglePageSimpleCurler(final SinglePageController singlePageDocumentView) {
        super(PageAnimationType.CURLER, singlePageDocumentView);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#getInitialXForBackFlip(int)
     */
    @Override
    protected int getInitialXForBackFlip(final int width) {
        return width;
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

        // Calculate point A
        mA.x = width - mMovement.x;
        mA.y = height;

        // Calculate point D
        mD.x = 0;
        mD.y = 0;
        if (mA.x > width / 2) {
            mD.x = width;
            mD.y = height - (width - mA.x) * height / mA.x;
        } else {
            mD.x = 2 * mA.x;
            mD.y = 0;
        }

        // Now calculate E and F taking into account that the line
        // AD is perpendicular to FB and EC. B and C are fixed points.
        final float tanA = (height - mD.y) / (mD.x + mMovement.x - width);
        final float _cos = (1 - tanA * tanA) / (1 + tanA * tanA);
        final float _sin = 2 * tanA / (1 + tanA * tanA);

        // And get F
        mF.x = (float) (width - mMovement.x + _cos * mMovement.x);
        mF.y = (float) (height - _sin * mMovement.x);

        // If the x position of A is above half of the page we are still not
        // folding the upper-right edge and so E and D are equal.
        if (mA.x > width / 2) {
            mE.x = mD.x;
            mE.y = mD.y;
        } else {
            // So get E
            mE.x = (float) (mD.x + _cos * (width - mD.x));
            mE.y = (float) -(_sin * (width - mD.x));
        }
    }
}
