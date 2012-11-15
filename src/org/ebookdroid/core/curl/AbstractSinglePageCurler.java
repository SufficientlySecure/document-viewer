package org.ebookdroid.core.curl;

import org.ebookdroid.core.EventDraw;
import org.ebookdroid.core.EventGLDraw;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.SinglePageController;
import org.ebookdroid.core.ViewState;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import org.emdev.ui.gl.GLCanvas;

public abstract class AbstractSinglePageCurler extends AbstractPageAnimator {

    /** Maximum radius a page can be flipped, by default it's the width of the view */
    protected float mFlipRadius;

    /** Page curl edge */
    protected Paint mCurlEdgePaint;

    /** Our points used to define the current clipping paths in our draw call */
    protected final Vector2D mB, mC, mD, mE, mF, mOldF, mOrigin;

    protected final Vector2D[] pageBack;
    protected final Vector2D[] pageBackShadow;

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

        pageBack = new Vector2D[] { mA, mD, mE, mF };
        pageBackShadow = new Vector2D[pageBack.length];
        for (int i = 0; i < pageBackShadow.length; i++) {
            pageBackShadow[i] = new Vector2D();
        }
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
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#onFirstDrawEvent(android.graphics.Canvas,
     *      org.ebookdroid.core.ViewState)
     */
    @Override
    protected void onFirstDrawEvent(final Canvas canvas, final ViewState viewState) {
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
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#onFirstDrawEvent(org.emdev.ui.gl.GLCanvas,
     *      org.ebookdroid.core.ViewState)
     */
    @Override
    protected void onFirstDrawEvent(final GLCanvas canvas, final ViewState viewState) {
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
    protected void drawForeground(final EventDraw event) {
        Page page = event.viewState.model.getPageObject(foreIndex);
        if (page == null) {
            page = event.viewState.model.getCurrentPageObject();
        }
        if (page != null) {
            event.canvas.save();
            event.canvas.clipRect(event.viewState.getBounds(page));
            event.process(page);
            event.canvas.restore();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#drawBackground(org.ebookdroid.core.EventDraw)
     */
    @Override
    protected void drawBackground(final EventDraw event) {
        final Path mask = createBackgroundPath();

        final Page page = event.viewState.model.getPageObject(backIndex);
        if (page != null) {
            // Save current canvas so we do not mess it up
            event.canvas.save();
            event.canvas.clipPath(mask);
            event.canvas.drawRect(event.canvas.getClipBounds(), event.viewState.paint.backgroundFillPaint);
            event.process(page);
            event.canvas.restore();
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#drawExtraObjects(org.ebookdroid.core.EventDraw)
     */
    @Override
    protected void drawExtraObjects(final EventDraw event) {
        final Path path = createCurlEdgePath();
        event.canvas.drawPath(path, mCurlEdgePaint);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#drawForeground(org.ebookdroid.core.EventDraw)
     */
    @Override
    protected void drawForeground(final EventGLDraw event) {
        Page page = event.viewState.model.getPageObject(foreIndex);
        if (page == null) {
            page = event.viewState.model.getCurrentPageObject();
        }
        if (page != null) {
            event.process(page);
        }
    }

    @Override
    protected void drawBackground(final EventGLDraw event) {
        if (foreIndex != backIndex) {
            final Page page = event.viewState.model.getPageObject(backIndex);
            if (page != null) {
                event.canvas.setClipPath(mA, mB, mC, mD);
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
    protected void drawExtraObjects(final EventGLDraw event) {
        final GLCanvas canvas = event.canvas;

        for (int i = 0; i < pageBackShadow.length; i++) {
            pageBackShadow[i].set(pageBack[i]);
        }
        pageBackShadow[0].y += 20;
        pageBackShadow[1].y -= 20;
        pageBackShadow[1].x += 20;
        pageBackShadow[2].y -= 20;
        pageBackShadow[2].x -= 20;
        pageBackShadow[2].x -= 20;

        canvas.save();
        canvas.setAlpha(0.5f);
        canvas.drawPoly(Color.BLACK, pageBackShadow);
        canvas.restore();

        canvas.drawPoly(mCurlEdgePaint.getColor(), pageBack);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#resetClipEdge()
     */
    @Override
    protected void resetClipEdge() {
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

    /**
     * Create a Path used as a mask to draw the background page
     *
     * @return
     */
    private Path createBackgroundPath() {
        final Path path = new Path();
        path.moveTo(mA.x, mA.y);
        path.lineTo(mB.x, mB.y);
        path.lineTo(mC.x, mC.y);
        path.lineTo(mD.x, mD.y);
        path.lineTo(mA.x, mA.y);
        return path;
    }

    /**
     * Creates a path used to draw the curl edge in.
     *
     * @return
     */
    private Path createCurlEdgePath() {
        final Path path = new Path();
        path.moveTo(mA.x, mA.y);
        path.lineTo(mD.x, mD.y);
        path.lineTo(mE.x, mE.y);
        path.lineTo(mF.x, mF.y);
        path.lineTo(mA.x, mA.y);
        return path;
    }

}
