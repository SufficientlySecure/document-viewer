package org.ebookdroid.core.curl;

import org.ebookdroid.core.Page;
import org.ebookdroid.core.PagePaint;
import org.ebookdroid.core.SinglePageDocumentView;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

public abstract class AbstractSinglePageCurler extends AbstractPageAnimator {

    /** Maximum radius a page can be flipped, by default it's the width of the view */
    protected float mFlipRadius;

    /** Page curl edge */
    protected Paint mCurlEdgePaint;

    /** Our points used to define the current clipping paths in our draw call */
    protected Vector2D mB, mC, mD, mE, mF, mOldF, mOrigin;

    public AbstractSinglePageCurler(final PageAnimationType type, final SinglePageDocumentView singlePageDocumentView) {
        super(type, singlePageDocumentView);
    }

    /**
     * Initialize the view
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
                point.y = (float) (Math.sin(Math.acos(Math.abs(point.x - mOrigin.x) / mFlipRadius)) * mFlipRadius);
            }
        }
        return point;
    }

    /**
     * Called on the first draw event of the view
     *
     * @param canvas
     */
    @Override
    protected void onFirstDrawEvent(final Canvas canvas, RectF viewRect) {
        mFlipRadius = viewRect.width();

        resetClipEdge();

        lock.writeLock().lock();
        try {
            updateValues();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Reset points to it's initial clip edge state
     */
    @Override
    protected void resetClipEdge() {
        // Set our base movement
        mMovement.x = mInitialEdgeOffset;
        mMovement.y = mInitialEdgeOffset;
        mOldMovement.x = 0;
        mOldMovement.y = 0;

        // Now set the points
        mA = new Vector2D(mInitialEdgeOffset, 0);
        mB = new Vector2D(view.getWidth(), view.getHeight());
        mC = new Vector2D(view.getWidth(), 0);
        mD = new Vector2D(0, 0);
        mE = new Vector2D(0, 0);
        mF = new Vector2D(0, 0);
        mOldF = new Vector2D(0, 0);

        // The movement origin point
        mOrigin = new Vector2D(view.getWidth(), 0);
    }

    /**
     * Draw the foreground
     *
     * @param canvas
     * @param rect
     * @param paint
     */
    @Override
    protected void drawForeground(final Canvas canvas, RectF viewRect) {
        Page page = view.getBase().getDocumentModel().getPageObject(foreIndex);
        if (page == null) {
            page = view.getBase().getDocumentModel().getCurrentPageObject();
        }
        if (page != null) {
            canvas.save();
            canvas.clipRect(page.getBounds());
            page.draw(canvas, viewRect, true);
            canvas.restore();
        }
    }

    /**
     * Draw the background image.
     *
     * @param canvas
     * @param rect
     * @param paint
     */
    @Override
    protected void drawBackground(final Canvas canvas, RectF viewRect) {
        final Path mask = createBackgroundPath();

        final Page page = view.getBase().getDocumentModel().getPageObject(backIndex);
        if (page != null) {
            // Save current canvas so we do not mess it up
            canvas.save();
            canvas.clipPath(mask);

            final PagePaint paint = !(view.getBase().getAppSettings().getNightMode()) ? PagePaint.NIGHT : PagePaint.DAY;

            canvas.drawRect(canvas.getClipBounds(), paint.getFillPaint());

            page.draw(canvas, viewRect, true);
            canvas.restore();
        }

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

    /**
     * Draw the curl page edge
     *
     * @param canvas
     */
    @Override
    protected void drawExtraObjects(final Canvas canvas, RectF viewRect) {
        final Path path = createCurlEdgePath();
        canvas.drawPath(path, mCurlEdgePaint);
    }

}
