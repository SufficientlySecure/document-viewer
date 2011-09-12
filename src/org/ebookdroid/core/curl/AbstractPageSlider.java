package org.ebookdroid.core.curl;

import org.ebookdroid.R;
import org.ebookdroid.core.PagePaint;
import org.ebookdroid.core.SinglePageDocumentView;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.utils.BitmapManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

public abstract class AbstractPageSlider extends AbstractPageAnimator {

    protected Bitmap arrowsBitmap;

    public AbstractPageSlider(final PageAnimationType type, final SinglePageDocumentView singlePageDocumentView) {
        super(type, singlePageDocumentView);
    }

    /**
     * Initialize specific value for the view
     */
    @Override
    public void init() {
        super.init();
        mInitialEdgeOffset = 0;
        arrowsBitmap = BitmapFactory.decodeResource(view.getBase().getContext().getResources(), R.drawable.arrows);
    }

    /**
     * Called on the first draw event of the view
     * 
     * @param canvas
     */
    @Override
    protected void onFirstDrawEvent(final Canvas canvas, final ViewState viewState) {
        lock.writeLock().lock();
        try {
            resetClipEdge();
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
    }

    /**
     * Do the page curl depending on the methods we are using
     */
    @Override
    protected void updateValues() {
        // Calculate point A
        mA.x = mMovement.x;
        mA.y = 0;
    }

    protected Bitmap getBitmap(final Canvas canvas) {
        Bitmap bitmap = BitmapManager.getBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.RGB_565);
        
        final PagePaint paint = !(SettingsManager.getAppSettings().getNightMode()) ? PagePaint.NIGHT : PagePaint.DAY;

        bitmap.eraseColor(paint.fillPaint.getColor());

        return bitmap;
    }

    @Override
    protected void drawExtraObjects(final Canvas canvas, final ViewState viewState) {
        final Paint paint = new Paint();
        paint.setFilterBitmap(true);
        paint.setAntiAlias(true);
        paint.setDither(true);
        canvas.drawBitmap(arrowsBitmap, view.getWidth() - arrowsBitmap.getWidth(),
                view.getHeight() - arrowsBitmap.getHeight(), paint);
    }

    @Override
    protected Vector2D fixMovement(final Vector2D movement, final boolean bMaintainMoveDir) {
        return movement;
    }

}
