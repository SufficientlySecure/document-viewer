package org.ebookdroid.core.curl;

import org.ebookdroid.core.EventGLDraw;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.SinglePageController;
import org.ebookdroid.core.ViewState;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import org.emdev.ui.gl.GLCanvas;

public abstract class AbstractSinglePageCurler extends AbstractPageAnimator {

    /** Maximum radius a page can be flipped, by default it's the width of the view */
    protected float mFlipRadius;

    /** Page curl edge */
    protected Paint mCurlEdgePaint;

    /** Our points used to define the current clipping paths in our draw call */
    protected final Vector2D mB, mC, mD, mE, mF, mOldF, mOrigin;

    protected final Vector2D[] foreBack;
    protected final Vector2D[] backClip;

    public AbstractSinglePageCurler(final PageAnimationType type, final SinglePageController singlePageDocumentView) {
        super(type, singlePageDocumentView);
        mB = new Vector2D();
        mC = new Vector2D();
        mD = new Vector2D();
        mE = new Vector2D();
        mF = new Vector2D();
        mOldF = new Vector2D();

        // The movement origin point
        mOrigin = new Vector2D(view.getWidth(), 0);

        foreBack = new Vector2D[] { mA, mD, mE, mF };
        backClip = new Vector2D[] { mA, mB, mC, mD };
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#init()
     */
    @Override
    public void init() {
        super.init();

        // Create our edge paint
        mCurlEdgePaint = new Paint();
        mCurlEdgePaint.setColor(Color.WHITE);
        mCurlEdgePaint.setAntiAlias(true);
        mCurlEdgePaint.setStyle(Paint.Style.FILL);
        mCurlEdgePaint.setShadowLayer(10, -5, 5, 0x99000000);

        mInitialEdgeOffset = 20;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#fixMovement(org.ebookdroid.core.curl.Vector2D, boolean)
     */
    @Override
    protected Vector2D fixMovement(Vector2D point, final boolean bMaintainMoveDir) {
        // Make sure we never ever move too much
        if (point.distance(mOrigin) > mFlipRadius) {
            if (bMaintainMoveDir) {
                // Maintain the direction
                point = mOrigin.sum(point.sub(mOrigin).normalize().mult(mFlipRadius));
            } else {
                // Change direction
                if (point.x > (mOrigin.x + mFlipRadius)) {
                    point.x = (mOrigin.x + mFlipRadius);
                } else if (point.x < (mOrigin.x - mFlipRadius)) {
                    point.x = (mOrigin.x - mFlipRadius);
                }
                point.y = 0;
            }
        }
        return point;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#onFirstDrawEvent(org.emdev.ui.gl.GLCanvas,
     *      org.ebookdroid.core.ViewState)
     */
    @Override
    protected final void onFirstDrawEvent(final GLCanvas canvas, final ViewState viewState) {
        mFlipRadius = viewState.viewRect.width();

        resetClipEdge();

        lock.writeLock().lock();
        try {
            updateValues();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#drawForeground(org.ebookdroid.core.EventDraw)
     */
    @Override
    protected final void drawForeground(final EventGLDraw event) {
        Page page = event.viewState.model.getPageObject(foreIndex);
        if (page == null) {
            page = event.viewState.model.getCurrentPageObject();
        }
        if (page != null) {
            event.process(page);
        }
    }

    @Override
    protected final void drawBackground(final EventGLDraw event) {
        if (foreIndex != backIndex) {
            final ViewState viewState = event.viewState;
            final Page page = viewState.model.getPageObject(backIndex);
            if (page != null) {
                event.canvas.setClipPath(backClip);

                final RectF viewRect = viewState.viewRect;
                final int color = viewState.paint.backgroundFillPaint.getColor();
                event.canvas.fillRect(0, 0, viewRect.width(), viewRect.height(), color);

                event.process(page);
                event.canvas.clearClipRect();
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#drawExtraObjects(org.ebookdroid.core.EventGLDraw)
     */
    @Override
    protected final void drawExtraObjects(final EventGLDraw event) {
        final GLCanvas canvas = event.canvas;

        shadow(canvas);

        canvas.fillPoly(mCurlEdgePaint.getColor(), foreBack);
        canvas.drawPoly(Color.BLACK, foreBack);
    }

    protected final void shadow(final GLCanvas canvas) {
        canvas.save();
        final float width = 40;
        final int count = 20;
        for (int i = count; i > 0; i--) {
            final float move = i * width / count;
            final float alpha = 0.5f * (1 - i / (float) count);

            canvas.setAlpha(alpha * alpha * alpha);
            canvas.translate(move, move);
            canvas.fillPoly(Color.BLACK, foreBack);
            canvas.translate(-move, -move);
        }
        canvas.restore();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#resetClipEdge()
     */
    @Override
    protected final void resetClipEdge() {
        // Set our base movement
        mMovement.x = mInitialEdgeOffset;
        mMovement.y = mInitialEdgeOffset;
        mOldMovement.x = 0;
        mOldMovement.y = 0;

        // Now set the points
        mA.set(mInitialEdgeOffset, 0);
        mB.set(view.getWidth(), view.getHeight());
        mC.set(view.getWidth(), 0);
        mD.set(0, 0);
        mE.set(0, 0);
        mF.set(0, 0);
        mOldF.set(0, 0);

        // The movement origin point
        mOrigin.set(view.getWidth(), 0);
    }
}
