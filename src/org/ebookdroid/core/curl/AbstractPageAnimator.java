package org.ebookdroid.core.curl;

import org.ebookdroid.R;
import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.core.EventDraw;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.SinglePageController;
import org.ebookdroid.core.ViewState;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class AbstractPageAnimator extends SinglePageView implements PageAnimator {

    protected static final Paint PAINT = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

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
    protected Vector2D mA = new Vector2D(0, 0);
    /** The initial offset for x and y axis movements */
    protected int mInitialEdgeOffset;
    /** The finger position */
    protected Vector2D mFinger;
    /** Movement point form the last frame */
    protected Vector2D mOldMovement;
    /** TRUE if the user moves the pages */
    protected boolean bUserMoves;

    protected BitmapRef foreBitmap;
    protected int foreBitmapIndex = -1;

    protected BitmapRef backBitmap;
    protected int backBitmapIndex = -1;

    protected Bitmap arrowsBitmap;

    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public AbstractPageAnimator(final PageAnimationType type, final SinglePageController singlePageDocumentView) {
        super(type, singlePageDocumentView);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.curl.SinglePageView#init()
     */
    @Override
    public void init() {
        super.init();
        arrowsBitmap = BitmapFactory.decodeResource(view.getBase().getContext().getResources(), R.drawable.arrows);

        mMovement = new Vector2D(0, 0);
        mFinger = new Vector2D(0, 0);
        mOldMovement = new Vector2D(0, 0);

        // Create our curl animation handler
        mAnimationHandler = new FlipAnimationHandler(this);

        // Set the default props
        mUpdateRate = 5;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.curl.SinglePageView#isPageVisible(org.ebookdroid.core.Page,
     *      org.ebookdroid.core.ViewState)
     */
    @Override
    public final boolean isPageVisible(final Page page, final ViewState viewState) {
        final int pageIndex = page.index.viewIndex;
        return pageIndex == this.foreIndex || pageIndex == this.backIndex;
    }

    /**
     * Swap to next view
     */
    protected ViewState nextView(final ViewState viewState) {
        if (viewState.model == null) {
            return viewState;
        }

        final int pageCount = viewState.model.getPageCount();

        foreIndex = viewState.pages.currentIndex % pageCount;
        backIndex = (foreIndex + 1) % pageCount;

        final Page forePage = viewState.model.getPageObject(foreIndex);
        final Page backPage = viewState.model.getPageObject(backIndex);
        return view.invalidatePages(viewState, forePage, backPage);
    }

    /**
     * Swap to previous view
     */
    protected ViewState previousView(final ViewState viewState) {
        if (viewState.model == null) {
            return viewState;
        }

        final int pageCount = viewState.model.getPageCount();

        backIndex = viewState.pages.currentIndex % pageCount;
        foreIndex = (pageCount + backIndex - 1) % pageCount;

        final Page forePage = viewState.model.getPageObject(foreIndex);
        final Page backPage = viewState.model.getPageObject(backIndex);
        return view.invalidatePages(viewState, forePage, backPage);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.curl.SinglePageView#flipAnimationStep()
     */
    @Override
    public synchronized void flipAnimationStep() {
        if (!bFlipping) {
            return;
        }

        // System.out.println("FAS start");
        final int width = view.getWidth();

        // No input when flipping
        bBlockTouchInput = true;

        // Handle speed
        float curlSpeed = width / 5;
        if (!bFlipRight) {
            curlSpeed *= -1;
        }

        // Move us
        mMovement.x += curlSpeed;
        mMovement = fixMovement(mMovement, false);

        // Create values

        lock.writeLock().lock();
        try {
            updateValues();

            if (mA.x < getLeftBound()) {
                mA.x = getLeftBound() - 1;
            }

            if (mA.x > width - 1) {
                mA.x = width;
            }
        } finally {
            lock.writeLock().unlock();
        }
        // Check for endings :D
        if (mA.x <= getLeftBound() || mA.x >= width - 1) {
            bFlipping = false;
            // System.out.println("FAS end");
            if (bFlipRight) {
                view.goToPage(backIndex);
                foreIndex = backIndex;
            } else {
                view.goToPage(foreIndex);
                backIndex = foreIndex;
            }

            // Create values
            lock.writeLock().lock();
            try {
                resetClipEdge();
                updateValues();
            } finally {
                lock.writeLock().unlock();
            }

            // Enable touch input after the next draw event
            bEnableInputAfterDraw = true;
        } else {
            mAnimationHandler.sleep(mUpdateRate);
        }

        // Force a new draw call
        view.redrawView();
    }

    protected float getLeftBound() {
        return 1;
    }

    protected abstract void resetClipEdge();

    protected abstract Vector2D fixMovement(Vector2D point, final boolean bMaintainMoveDir);

    protected abstract void drawBackground(EventDraw event);

    protected abstract void drawForeground(EventDraw event);

    protected abstract void drawExtraObjects(EventDraw event);

    /**
     * Update points values values.
     */
    protected abstract void updateValues();

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.curl.SinglePageView#draw(org.ebookdroid.core.EventDraw)
     */
    @Override
    public final synchronized void draw(final EventDraw event) {
        final Canvas canvas = event.canvas;
        final ViewState viewState = event.viewState;

        if (!enabled()) {
            BitmapManager.release(foreBitmap);
            BitmapManager.release(backBitmap);

            foreBitmap = null;
            backBitmap = null;

            super.draw(event);
            return;
        }

        // We need to initialize all size data when we first draw the view
        if (!isViewDrawn()) {
            setViewDrawn(true);
            onFirstDrawEvent(canvas, viewState);
        }

        canvas.drawColor(Color.BLACK);

        // Draw our elements
        lock.readLock().lock();
        try {
            drawInternal(event);
            drawExtraObjects(event);
        } finally {
            lock.readLock().unlock();
        }

        // Check if we can re-enable input
        if (bEnableInputAfterDraw) {
            bBlockTouchInput = false;
            bEnableInputAfterDraw = false;
        }
    }

    protected void drawInternal(final EventDraw event) {
        drawForeground(event);
        if (foreIndex != backIndex) {
            drawBackground(event);
        }
    }

    protected abstract void onFirstDrawEvent(Canvas canvas, final ViewState viewState);

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.curl.SinglePageView#enabled()
     */
    @Override
    public final boolean enabled() {
        final Rect limits = view.getScrollLimits();
        return limits.width() <= 0 && limits.height() <= 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.curl.SinglePageView#onTouchEvent(android.view.MotionEvent)
     */
    @Override
    public final boolean onTouchEvent(final MotionEvent event) {
        if (!bBlockTouchInput) {

            // Get our finger position
            mFinger.x = event.getX();
            mFinger.y = event.getY();
            final int width = view.getWidth();

            ViewState viewState = new ViewState(view);

            // Depending on the action do what we need to
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mOldMovement.x = mFinger.x;
                    mOldMovement.y = mFinger.y;
                    bUserMoves = false;
                    return false;
                case MotionEvent.ACTION_UP:
                    if (bUserMoves) {
                        bUserMoves = false;
                        bFlipping = true;
                        flipAnimationStep();
                    } else {
                        return false;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if ((mFinger.distanceSquared(mOldMovement) > 625)) {
                        if (!bUserMoves) {
                            // If we moved over the half of the display flip to next
                            if (mOldMovement.x > (width >> 1)) {
                                mMovement.x = mInitialEdgeOffset;
                                mMovement.y = mInitialEdgeOffset;

                                // Set the right movement flag
                                bFlipRight = true;
                                viewState = nextView(viewState);

                            } else {
                                // Set the left movement flag
                                bFlipRight = false;

                                // go to next previous page
                                viewState = previousView(viewState);

                                // Set new movement
                                mMovement.x = getInitialXForBackFlip(width);
                                mMovement.y = mInitialEdgeOffset;
                            }
                        }
                        bUserMoves = true;
                    } else {
                        if (!bUserMoves) {
                            break;
                        }
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
                    lock.writeLock().lock();
                    try {
                        updateValues();
                    } finally {
                        lock.writeLock().unlock();
                    }
                    view.redrawView(viewState);
                    return !bUserMoves;
            }

        }

        // TODO: Only consume event if we need to.
        return true;
    }

    protected int getInitialXForBackFlip(final int width) {
        return width;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.curl.SinglePageView#pageUpdated(int)
     */
    @Override
    public void pageUpdated(final ViewState viewState, final Page page) {
        if (foreBitmapIndex == page.index.viewIndex) {
            foreBitmapIndex = -1;
        }
        if (backBitmapIndex == page.index.viewIndex) {
            backBitmapIndex = -1;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.curl.SinglePageView#animate(int)
     */
    @Override
    public void animate(final int direction) {
        resetClipEdge();
        mMovement = new Vector2D(direction < 0 ? 7 * view.getWidth() / 8 : view.getWidth() / 8, mInitialEdgeOffset);
        bFlipping = true;
        bFlipRight = direction > 0;
        final ViewState viewState = new ViewState(view);
        if (bFlipRight) {
            nextView(viewState);
        } else {
            previousView(viewState);
        }

        bUserMoves = false;
        flipAnimationStep();

    }

}
