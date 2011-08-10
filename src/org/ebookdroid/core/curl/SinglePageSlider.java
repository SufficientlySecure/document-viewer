package org.ebookdroid.core.curl;

import org.ebookdroid.core.Page;
import org.ebookdroid.core.SinglePageDocumentView;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class SinglePageSlider extends AbstractPageAnimator {

    private Bitmap bitmap;

    public SinglePageSlider(final SinglePageDocumentView singlePageDocumentView) {
        super(singlePageDocumentView);
    }

    /**
     * Initialize specific value for the view
     */
    @Override
    public final void init() {
        super.init();
        mInitialEdgeOffset = 0;

    }

    /**
     * Called on the first draw event of the view
     * 
     * @param canvas
     */
    protected void onFirstDrawEvent(final Canvas canvas) {
        resetClipEdge();
        updateValues();
    }

    /**
     * Reset points to it's initial clip edge state
     */
    protected void resetClipEdge() {
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
    protected void updateValues() {
        // Calculate point A
        mA.x = mMovement.x;
        mA.y = 0;
    }

    /**
     * Draw the foreground
     * 
     * @param canvas
     * @param rect
     * @param paint
     */
    @Override
    protected void drawForeground(final Canvas canvas) {
        Page page = view.getBase().getDocumentModel().getPageObject(foreIndex);
        if (page == null) {
            page = view.getBase().getDocumentModel().getCurrentPageObject();
        }
        if (page != null) {
            Bitmap fore = getBitmap(canvas);
            Canvas tmp = new Canvas(fore);
            page.draw(tmp, true);

            Rect src = new Rect((int) mA.x, 0, view.getWidth(), view.getHeight());
            RectF dst = new RectF(0, 0, view.getWidth() - mA.x, view.getHeight());
            // Rect src = new Rect(0, 0, view.getWidth(), view.getHeight());
            // RectF dst = new RectF(0, 0, view.getWidth() - mA.x, view.getHeight());
            final Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            paint.setDither(true);
            canvas.drawBitmap(fore, src, dst, paint);
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
    protected void drawBackground(final Canvas canvas) {
        final Page page = view.getBase().getDocumentModel().getPageObject(backIndex);
        if (page != null) {
            Bitmap back = getBitmap(canvas);
            Canvas tmp = new Canvas(back);
            page.draw(tmp, true);

            // Rect src = new Rect(0, 0, view.getWidth(), view.getHeight());
            // RectF dst = new RectF(view.getWidth() - mA.x, 0, view.getWidth(), view.getHeight());
            final Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            paint.setDither(true);
            Rect src = new Rect(0, 0, (int) mA.x, view.getHeight());
            RectF dst = new RectF(view.getWidth() - mA.x, 0, view.getWidth(), view.getHeight());
            canvas.drawBitmap(back, src, dst, paint);
        }

    }

    private Bitmap getBitmap(final Canvas canvas) {
        if (bitmap == null || bitmap.getWidth() != canvas.getWidth() || bitmap.getHeight() != canvas.getHeight()) {
            if (bitmap != null) {
                bitmap.recycle();
            }
            bitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.RGB_565);
        }
        return bitmap;
    }

    @Override
    protected void drawExtraObjects(Canvas canvas) {
    }

    @Override
    protected Vector2D fixMovement(Vector2D movement, boolean bMaintainMoveDir) {
        return movement;
    }

}
