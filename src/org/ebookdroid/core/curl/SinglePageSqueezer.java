package org.ebookdroid.core.curl;

import org.ebookdroid.core.Page;
import org.ebookdroid.core.SinglePageDocumentView;

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
    protected void drawForeground(final Canvas canvas, RectF viewRect) {
        Page page = view.getBase().getDocumentModel().getPageObject(foreIndex);
        if (page == null) {
            page = view.getBase().getDocumentModel().getCurrentPageObject();
        }
        if (page != null) {
            final Bitmap fore = getBitmap(canvas);
            final Canvas tmp = new Canvas(fore);
            page.draw(tmp, viewRect, true);

            final Rect src = new Rect(0, 0, (int)viewRect.width(), (int)viewRect.height());
            final RectF dst = new RectF(0, 0, viewRect.width() - mA.x, viewRect.height());
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
    protected void drawBackground(final Canvas canvas, RectF viewRect) {
        final Page page = view.getBase().getDocumentModel().getPageObject(backIndex);
        if (page != null) {
            final Bitmap back = getBitmap(canvas);
            final Canvas tmp = new Canvas(back);
            page.draw(tmp, viewRect, true);

            final Rect src = new Rect(0, 0, (int)viewRect.width(), (int)viewRect.height());
            final RectF dst = new RectF(viewRect.width() - mA.x, 0, viewRect.width(), viewRect.height());
            final Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            paint.setDither(true);
            canvas.drawBitmap(back, src, dst, paint);
        }

    }

}
