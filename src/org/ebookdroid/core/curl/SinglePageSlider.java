package org.ebookdroid.core.curl;

import org.ebookdroid.core.Page;
import org.ebookdroid.core.PagePaint;
import org.ebookdroid.core.SinglePageDocumentView;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;

/**
 * The Class SinglePageCurler.
 * 
 * Used for drawing page curl animation
 * 
 * @author Moritz 'Moss' Wundke (b.thax.dcg@gmail.com)
 * 
 */
public class SinglePageSlider implements PageAnimator {

    /** Px / Draw call */
    private int mCurlSpeed;

    /** Fixed update time used to create a smooth curl animation */
    private int mUpdateRate;

    /** The initial offset for x and y axis movements */
    private int mInitialEdgeOffset;

    /** Handler used to auto flip time based */
    private FlipAnimationHandler mAnimationHandler;
    /** Point used to move */
    private Vector2D mMovement;

    /** The finger position */
    private Vector2D mFinger;

    /** Movement point form the last frame */
    private Vector2D mOldMovement;

    /** If false no draw call has been done */
    private boolean bViewDrawn;

    /** Defines the flip direction that is currently considered */
    private boolean bFlipRight;

    /** If TRUE we are currently auto-flipping */
    private boolean bFlipping;

    /** TRUE if the user moves the pages */
    private boolean bUserMoves;

    /** Used to control touch input blocking */
    private boolean bBlockTouchInput = false;

    /** Enable input after the next draw event */
    private boolean bEnableInputAfterDraw = false;

    private final SinglePageDocumentView view;

    private int foreIndex = -1;

    private int backIndex = -1;

    private Vector2D mA;

    public SinglePageSlider(final SinglePageDocumentView singlePageDocumentView) {
        this.view = singlePageDocumentView;
    }

    /**
     * Initialize the view
     */
    public final void init() {
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
        mInitialEdgeOffset = 0;

    }

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
                        mMovement.x = width;
                        mMovement.y = mInitialEdgeOffset;
                    }

                    break;
                case MotionEvent.ACTION_UP:
                    if (bUserMoves) {
                        bUserMoves = false;
                        bFlipping = true;
                        FlipAnimationStep();
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    bUserMoves = true;

                    // Get movement
                    mMovement.x -= mFinger.x - mOldMovement.x;
                    mMovement.y -= mFinger.y - mOldMovement.y;

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

        // Check if we can re-enable input
        if (bEnableInputAfterDraw) {
            bBlockTouchInput = false;
            bEnableInputAfterDraw = false;
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

            ResetClipEdge();

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

    /**
     * Swap to next view
     */
    private void nextView() {
        foreIndex = view.getCurrentPage();
        if (foreIndex >= view.getBase().getDocumentModel().getPageCount()) {
            foreIndex = 0;
        }
        backIndex = foreIndex + 1;
        if (backIndex >= view.getBase().getDocumentModel().getPageCount()) {
            backIndex = 0;
        }
        // view.goToPageImpl(foreIndex);
    }

    /**
     * Swap to previous view
     */
    private void previousView() {
        backIndex = view.getCurrentPage();
        foreIndex = backIndex - 1;
        if (foreIndex < 0) {
            foreIndex = view.getBase().getDocumentModel().getPages().size() - 1;
        }
        // view.goToPageImpl(foreIndex);
    }

    /**
     * Called on the first draw event of the view
     * 
     * @param canvas
     */
    protected void onFirstDrawEvent(final Canvas canvas) {
        ResetClipEdge();
        updateValues();
    }

    /**
     * Reset points to it's initial clip edge state
     */
    public void ResetClipEdge() {
        // Set our base movement
        mMovement.x = mInitialEdgeOffset;
        mMovement.y = mInitialEdgeOffset;
        mOldMovement.x = 0;
        mOldMovement.y = 0;

        // Now set the points
        mA = new Vector2D(mInitialEdgeOffset, 0);
    }

    /**
     * Do the page curl depending on the methods we are using
     */
    private void updateValues() {
        final int width = view.getWidth();
        final int height = view.getHeight();

        // Calculate point A
        mA.x = mMovement.x;
        mA.y = height;
    }

 
    /**
     * Draw the foreground
     * 
     * @param canvas
     * @param rect
     * @param paint
     */
    private void drawForeground(final Canvas canvas) {
        Page page = view.getBase().getDocumentModel().getPageObject(foreIndex);
        if (page == null) {
            page = view.getBase().getDocumentModel().getCurrentPageObject();
        }
        if (page != null) {
            Bitmap fore = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.RGB_565);
            Canvas tmp = new Canvas(fore);
            page.draw(tmp, true);
            
            Rect src = new Rect((int)mA.x, 0, view.getWidth(), view.getHeight());
            RectF dst = new RectF(0, 0, view.getWidth() - mA.x, view.getHeight());
//            Rect src = new Rect(0, 0, view.getWidth(), view.getHeight());
//            RectF dst = new RectF(0, 0, view.getWidth() - mA.x, view.getHeight());
            final Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            paint.setDither(true);
            canvas.drawBitmap(fore, src, dst, paint);
            fore.recycle();
        }
    }

    /**
     * Draw the background image.
     * 
     * @param canvas
     * @param rect
     * @param paint
     */
    private void drawBackground(final Canvas canvas) {
        final Page page = view.getBase().getDocumentModel().getPageObject(backIndex);
        if (page != null) {
            Bitmap back = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.RGB_565);
            Canvas tmp = new Canvas(back);
            page.draw(tmp, true);
            
//            Rect src = new Rect(0, 0, view.getWidth(), view.getHeight());
//            RectF dst = new RectF(view.getWidth() - mA.x, 0, view.getWidth(), view.getHeight());
            final Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            paint.setDither(true);
            Rect src = new Rect(0, 0, (int)mA.x, view.getHeight());
            RectF dst = new RectF(view.getWidth() - mA.x, 0, view.getWidth(), view.getHeight());
            canvas.drawBitmap(back, src, dst, paint );
            back.recycle();
        }

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
}
