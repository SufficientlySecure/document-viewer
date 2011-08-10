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
        super(singlePageDocumentView);
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

            Rect src = new Rect(0, 0, view.getWidth(), view.getHeight());
            RectF dst = new RectF(0, 0, view.getWidth() - mA.x, view.getHeight());
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

            Rect src = new Rect(0, 0, view.getWidth(), view.getHeight());
            RectF dst = new RectF(view.getWidth() - mA.x, 0, view.getWidth(), view.getHeight());
            final Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            paint.setDither(true);
            canvas.drawBitmap(back, src, dst, paint);
        }

    }

}
