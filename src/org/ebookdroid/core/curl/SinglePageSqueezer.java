package org.ebookdroid.core.curl;

import org.ebookdroid.core.Page;
import org.ebookdroid.core.SinglePageDocumentView;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.utils.BitmapManager;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class SinglePageSqueezer extends AbstractPageSlider {

    public SinglePageSqueezer(final SinglePageDocumentView singlePageDocumentView) {
        super(PageAnimationType.SQUEEZER, singlePageDocumentView);
    }

    /**
     * Draw the foreground
     * 
     * @param canvas
     * @param rect
     * @param paint
     */
    @Override
    protected void drawForeground(final Canvas canvas, final ViewState viewState) {
        Page page = view.getBase().getDocumentModel().getPageObject(foreIndex);
        if (page == null) {
            page = view.getBase().getDocumentModel().getCurrentPageObject();
        }
        if (page != null) {
            final Bitmap fore = getBitmap(canvas);
            final Canvas tmp = new Canvas(fore);
            page.draw(tmp, viewState, true);

            final Rect src = new Rect(0, 0, (int) viewState.viewRect.width(), (int) viewState.viewRect.height());
            final RectF dst = new RectF(0, 0, viewState.viewRect.width() - mA.x, viewState.viewRect.height());
            final Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            paint.setDither(true);
            canvas.drawBitmap(fore, src, dst, paint);
            BitmapManager.recycle(fore);
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
    protected void drawBackground(final Canvas canvas, final ViewState viewState) {
        final Page page = view.getBase().getDocumentModel().getPageObject(backIndex);
        if (page != null) {
            final Bitmap back = getBitmap(canvas);
            final Canvas tmp = new Canvas(back);
            page.draw(tmp, viewState, true);

            final Rect src = new Rect(0, 0, (int) viewState.viewRect.width(), (int) viewState.viewRect.height());
            final RectF dst = new RectF(viewState.viewRect.width() - mA.x, 0, viewState.viewRect.width(),
                    viewState.viewRect.height());
            final Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            paint.setDither(true);
            canvas.drawBitmap(back, src, dst, paint);
            BitmapManager.recycle(back);
        }

    }

}
