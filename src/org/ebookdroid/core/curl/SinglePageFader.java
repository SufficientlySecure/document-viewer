package org.ebookdroid.core.curl;

import org.ebookdroid.core.EventDraw;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.SinglePageController;

import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class SinglePageFader extends AbstractPageSlider {

    private final Paint paint = new Paint(PAINT);
    
    public SinglePageFader(final SinglePageController singlePageDocumentView) {
        super(PageAnimationType.FADER, singlePageDocumentView);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#drawForeground(org.ebookdroid.core.EventDraw)
     */
    @Override
    protected void drawForeground(EventDraw event) {
        Page page = event.viewState.model.getPageObject(foreIndex);
        if (page == null) {
            page = event.viewState.model.getCurrentPageObject();
        }
        if (page != null) {
            event.process(page);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#drawBackground(org.ebookdroid.core.EventDraw)
     */
    @Override
    protected void drawBackground(EventDraw event) {
        final Page page = event.viewState.model.getPageObject(backIndex);
        if (page != null) {

            updateBackBitmap(event, page);

            RectF viewRect = event.viewState.viewRect;

            final Rect src = new Rect(0, 0, (int) viewRect.width(), (int) viewRect.height());
            final RectF dst = new RectF(0, 0, viewRect.width(), viewRect.height());

            paint.setAlpha(255 * (int) mA.x / (int) viewRect.width());

            event.canvas.drawBitmap(backBitmap.getBitmap(), src, dst, paint);
        }
    }
}
