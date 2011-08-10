package org.ebookdroid.core.curl;

import org.ebookdroid.core.SinglePageDocumentView;

/**
 * The Class SinglePageCurler.
 *
 * Used for drawing page curl animation
 *
 * @author Moritz 'Moss' Wundke (b.thax.dcg@gmail.com)
 *
 */
public class SinglePageDynamicCurler extends AbstractSinglePageCurler {

    public SinglePageDynamicCurler(final SinglePageDocumentView singlePageDocumentView) {
        super(singlePageDocumentView);
    }

    @Override
    protected int getInitialXForBackFlip(final int width) {
        return width << 1;
    }

    /**
     * Do the page curl depending on the methods we are using
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

        final float BH = (float) (Math.sqrt(deltaX * deltaX + deltaY * deltaY) / 2);
        final double tangAlpha = deltaY / deltaX;
        final double alpha = Math.atan(deltaY / deltaX);
        final double _cos = Math.cos(alpha);
        final double _sin = Math.sin(alpha);

        mA.x = (float) (width - (BH / _cos));
        mA.y = height;

        mD.y = (float) (height - (BH / _sin));
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
            mD.x = width + (float) (tangAlpha * mD.y);
            mE.y = 0;
            mE.x = width + (float) (Math.tan(2 * alpha) * mD.y);
        }
    }

}
