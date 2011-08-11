package org.ebookdroid.core.curl;

import org.ebookdroid.core.SinglePageDocumentView;

import android.graphics.Canvas;
import android.graphics.Color;
import android.view.MotionEvent;

public abstract class AbstractPageAnimator implements PageAnimator {

    /** If false no draw call has been done */
    boolean bViewDrawn;
    protected int foreIndex = -1;
    protected int backIndex = -1;
    protected final SinglePageDocumentView view;
    /** Px / Draw call */
    protected int mCurlSpeed;
    /** Fixed update time used to create a smooth curl animation */
    protected int mUpdateRate;
    /** Handler used to auto flip time based */
    protected FlipAnimationHandler mAnimationHandler;
    /** Point used to move */
    protected Vector2D mMovement;
    /** Defines the flip direction that is currently considered */
    protected boolean bFlipRight;
    /** If TRUE we are currently auto-flipping */
    protected boolean bFlipping;
    /** Used to control touch input blocking */
    protected boolean bBlockTouchInput = false;
    /** Enable input after the next draw event */
    protected boolean bEnableInputAfterDraw = false;
    protected Vector2D mA;
    /** The initial offset for x and y axis movements */
    protected int mInitialEdgeOffset;
    /** The finger position */
    protected Vector2D mFinger;
    /** Movement point form the last frame */
    protected Vector2D mOldMovement;
    /** TRUE if the user moves the pages */
    protected boolean bUserMoves;
    

    public AbstractPageAnimator(SinglePageDocumentView singlePageDocumentView) {
        this.view = singlePageDocumentView;
    }

    public void init() {
        // The focus flags are needed
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);

        mMovement = new Vector2D(0, 0);
        mFinger = new Vector2D(0, 0);
        mOldMovement = new Vector2D(0, 0);

        // Create our curl animation handler
        mAnimationHandler = new FlipAnimationHandler(this);

        // Set the default props
        mCurlSpeed = 30;
        mUpdateRate = 33;
    }

    public void setViewDrawn(final boolean bViewDrawn) {
        this.bViewDrawn = bViewDrawn;
    }

    public boolean isViewDrawn() {
        return bViewDrawn;
    }

    /**
     * Reset page indexes.
     */
    public void resetPageIndexes() {
        foreIndex = backIndex = -1;
    }

    public int getForeIndex() {
        return foreIndex;
    }

    public int getBackIndex() {
        return backIndex;
    }

    /**
     * Swap to next view
     */
    protected void nextView() {
        foreIndex = view.getCurrentPage();
        if (foreIndex >= view.getBase().getDocumentModel().getPageCount()) {
            foreIndex = 0;
        }
        backIndex = foreIndex + 1;
        if (backIndex >= view.getBase().getDocumentModel().getPageCount()) {
            backIndex = 0;
        }
    }

    /**
     * Swap to previous view
     */
    protected void previousView() {
        backIndex = view.getCurrentPage();
        foreIndex = backIndex - 1;
        if (foreIndex < 0) {
            foreIndex = view.getBase().getDocumentModel().getPages().size() - 1;
        }
    }

    /**
     * Execute a step of the flip animation
     */
    public void FlipAnimationStep() {
        if (!bFlipping) {
            return;
        }

        final int width = view.getWidth();

        // No input when flipping
        bBlockTouchInput = true;

        // Handle speed
        float curlSpeed = mCurlSpeed;
        if (!bFlipRight) {
            curlSpeed *= -1;
        }

        // Move us
        mMovement.x += curlSpeed;
        mMovement = fixMovement(mMovement, false);

        // Create values
        updateValues();

        // Check for endings :D
        if (mA.x < 1 || mA.x > width - 1) {
            bFlipping = false;
            if (bFlipRight) {
                view.goToPageImpl(backIndex);
            } else {
                view.goToPageImpl(foreIndex);
            }

            resetClipEdge();

            // Create values
            updateValues();

            // Enable touch input after the next draw event
            bEnableInputAfterDraw = true;
        } else {
            mAnimationHandler.sleep(mUpdateRate);
        }

        // Force a new draw call
        view.invalidate();
    }

    protected abstract void resetClipEdge();

    protected abstract Vector2D fixMovement(Vector2D point, final boolean bMaintainMoveDir);

    protected abstract void drawBackground(final Canvas canvas);

    protected abstract void drawForeground(final Canvas canvas);

    protected abstract void drawExtraObjects(final Canvas canvas);

    /**
     * Update points values values.
     */
    protected abstract void updateValues();

    /*
     * (non-Javadoc)
     * 
     * @see org.ebookdroid.core.curl.PageAnimator#onDraw(android.graphics.Canvas)
     */
    public void onDraw(final Canvas canvas) {
        // We need to initialize all size data when we first draw the view
        if (!isViewDrawn()) {
            setViewDrawn(true);
            onFirstDrawEvent(canvas);
        }

        canvas.drawColor(Color.BLACK);

        // Draw our elements
        drawForeground(canvas);
        drawBackground(canvas);
        drawExtraObjects(canvas);

        // Check if we can re-enable input
        if (bEnableInputAfterDraw) {
            bBlockTouchInput = false;
            bEnableInputAfterDraw = false;
        }
    }

    protected abstract void onFirstDrawEvent(Canvas canvas);

    public boolean onTouchEvent(final MotionEvent event) {
        if (!bBlockTouchInput) {

            // Get our finger position
            mFinger.x = event.getX();
            mFinger.y = event.getY();
            final int width = view.getWidth();

            // Depending on the action do what we need to
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mOldMovement.x = mFinger.x;
                    mOldMovement.y = mFinger.y;
                    bUserMoves = false;
                    break;
                case MotionEvent.ACTION_UP:
                    if (bUserMoves) {
                        bUserMoves = false;
                        bFlipping = true;
                        FlipAnimationStep();
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mFinger.distanceSquared(mOldMovement) > 625) {
                        if (!bUserMoves) {
                            // If we moved over the half of the display flip to next
                            if (mOldMovement.x > (width >> 1)) {
                                mMovement.x = mInitialEdgeOffset;
                                mMovement.y = mInitialEdgeOffset;
    
                                // Set the right movement flag
                                bFlipRight = true;
                                nextView();
    
                            } else {
                                // Set the left movement flag
                                bFlipRight = false;
    
                                // go to next previous page
                                previousView();
    
                                // Set new movement
                                mMovement.x = getInitialXForBackFlip(width);
                                mMovement.y = mInitialEdgeOffset;
                            }
                        }
                        bUserMoves = true;
                    } else {
                        break;
                    }

                    // Get movement
                    mMovement.x -= mFinger.x - mOldMovement.x;
                    mMovement.y -= mFinger.y - mOldMovement.y;
                    mMovement = fixMovement(mMovement, true);

                    // Make sure the y value get's locked at a nice level
                    if (mMovement.y <= 1) {
                        mMovement.y = 1;
                    }

                    // Get movement direction
                    if (mFinger.x < mOldMovement.x) {
                        bFlipRight = true;
                    } else {
                        bFlipRight = false;
                    }

                    // Save old movement values
                    mOldMovement.x = mFinger.x;
                    mOldMovement.y = mFinger.y;

                    // Force a new draw call
                    updateValues();
                    view.invalidate();
                    break;
            }

        }

        // TODO: Only consume event if we need to.
        return true;
    }

    protected int getInitialXForBackFlip(int width) {
        return width;
    }
}
