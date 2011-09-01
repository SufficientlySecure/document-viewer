package org.ebookdroid.core.curl;

import org.ebookdroid.core.Page;
import org.ebookdroid.core.SinglePageDocumentView;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class SinglePageFader extends AbstractPageSlider {

    public SinglePageFader(final SinglePageDocumentView singlePageDocumentView) {
        super(PageAnimationType.FADER, singlePageDocumentView);
    }

    /**
     * Draw the foreground
     *
     * @param canvas
     * @param rect
     * @param paint
     */
    @Override
    protected void drawForeground(final Canvas canvas, RectF viewRect, final float zoom) {
        Page page = view.getBase().getDocumentModel().getPageObject(foreIndex);
        if (page == null) {
            page = view.getBase().getDocumentModel().getCurrentPageObject();
        }
        if (page != null) {
            page.draw(canvas, viewRect, zoom, true);
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
    protected void drawBackground(final Canvas canvas, RectF viewRect, final float zoom) {
        final Page page = view.getBase().getDocumentModel().getPageObject(backIndex);
        if (page != null) {
            final Bitmap back = getBitmap(canvas);
            final Canvas tmp = new Canvas(back);
            page.draw(tmp, viewRect, zoom, true);

            final Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            paint.setDither(true);
            paint.setAlpha(255 * (int) mA.x / (int)viewRect.width());
            final Rect src = new Rect(0, 0, (int)viewRect.width(), (int)viewRect.height());
            final RectF dst = new RectF(0, 0, viewRect.width(), viewRect.height());
            canvas.drawBitmap(back, src, dst, paint);
        }

    }

}
