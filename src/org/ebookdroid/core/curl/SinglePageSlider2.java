package org.ebookdroid.core.curl;

import org.ebookdroid.core.EventDraw;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.SinglePageController;

import android.graphics.Rect;
import android.graphics.RectF;

public class SinglePageSlider2 extends AbstractPageSlider {

    public SinglePageSlider2(final SinglePageController singlePageDocumentView) {
        super(PageAnimationType.SLIDER, singlePageDocumentView);
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
            updateForeBitmap(event, page);

            final RectF viewRect = event.viewState.viewRect;

            final Rect src = new Rect((int) mA.x, 0, (int) viewRect.width(), (int) viewRect.height());
            final RectF dst = new RectF(0, 0, viewRect.width() - mA.x, viewRect.height());

            foreBitmap.draw(event.canvas, src, dst, PAINT);
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

            final RectF viewRect = event.viewState.viewRect;

            final Rect src = new Rect((int)(view.getWidth() - mA.x), 0, (int) view.getWidth(), view.getHeight());
            final RectF dst = new RectF(viewRect.width() - mA.x, 0, viewRect.width(), viewRect.height());

            backBitmap.draw(event.canvas, src, dst, PAINT);
        }
    }
}
