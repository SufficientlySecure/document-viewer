package org.ebookdroid.core.curl;

import org.ebookdroid.core.Page;
import org.ebookdroid.core.PagePaint;
import org.ebookdroid.core.SinglePageDocumentView;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.core.bitmaps.BitmapManager;
import org.ebookdroid.core.bitmaps.BitmapRef;
import org.ebookdroid.core.settings.SettingsManager;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public abstract class AbstractPageSlider extends AbstractPageAnimator {

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

    protected BitmapRef getBitmap(final Canvas canvas, final ViewState viewState, final BitmapRef ref) {
        BitmapRef bitmap = ref;
        if (ref == null || ref.clearEmptyRef() || ref.width != canvas.getWidth() || ref.height != canvas.getHeight()) {
            BitmapManager.release(ref);
            bitmap = BitmapManager.getBitmap("Curler image", canvas.getWidth(), canvas.getHeight(), Bitmap.Config.RGB_565);
        }
        final PagePaint paint = viewState.nightMode ? PagePaint.NIGHT : PagePaint.DAY;
        bitmap.getBitmap().eraseColor(paint.backgroundFillPaint.getColor());
        return bitmap;
    }

    @Override
    protected void drawExtraObjects(final Canvas canvas, final ViewState viewState) {
        final Paint paint = new Paint();
        paint.setFilterBitmap(true);
        paint.setAntiAlias(true);
        paint.setDither(true);

        if (SettingsManager.getAppSettings().getShowAnimIcon()) {
            canvas.drawBitmap(arrowsBitmap, view.getWidth() - arrowsBitmap.getWidth(),
                    view.getHeight() - arrowsBitmap.getHeight(), paint);
        }
    }

    @Override
    protected Vector2D fixMovement(final Vector2D movement, final boolean bMaintainMoveDir) {
        return movement;
    }

    protected final void updateForeBitmap(final Canvas canvas, final ViewState viewState, Page page) {
        if (foreBitmapIndex != foreIndex || foreBitmap == null) {
            foreBitmap = getBitmap(canvas, viewState, foreBitmap);

            // if (LCTX.isDebugEnabled()) {
            // LCTX.d("updateForeBitmap(): " +page.index.viewIndex);
            // }
            final Canvas tmp = new Canvas(foreBitmap.getBitmap());
            page.draw(tmp, viewState, true);
            foreBitmapIndex = page.index.viewIndex;
        }
    }

    protected final void updateBackBitmap(final Canvas canvas, final ViewState viewState, Page page) {
        if (backBitmapIndex != backIndex || backBitmap == null) {
            backBitmap = getBitmap(canvas, viewState, backBitmap);

            // if (LCTX.isDebugEnabled()) {
            // LCTX.d("updateBackBitmap(): " +page.index.viewIndex);
            // }
            final Canvas tmp = new Canvas(backBitmap.getBitmap());
            page.draw(tmp, viewState, true);
            backBitmapIndex = page.index.viewIndex;
        }
    }

}
