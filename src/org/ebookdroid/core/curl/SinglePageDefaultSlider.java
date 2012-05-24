package org.ebookdroid.core.curl;

import org.ebookdroid.core.EventDraw;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.SinglePageController;
import org.ebookdroid.core.ViewState;

import android.graphics.Rect;
import android.graphics.RectF;

public class SinglePageDefaultSlider extends AbstractPageSlider {

    public SinglePageDefaultSlider(final SinglePageController singlePageDocumentView) {
        super(PageAnimationType.NONE, singlePageDocumentView);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.PageAnimator#isPageVisible(org.ebookdroid.core.Page, org.ebookdroid.core.ViewState)
     */
    @Override
    public boolean isPageVisible(final Page page, final ViewState viewState) {
        final int pageIndex = page.index.viewIndex;
        return pageIndex == viewState.model.getCurrentViewPageIndex();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#drawForeground(org.ebookdroid.core.EventDraw)
     */
    @Override
    protected void drawForeground(final EventDraw event) {
        final ViewState viewState = event.viewState;
        Page page = null;
        if (bFlipping) {
            page = viewState.model.getPageObject(!bFlipRight ? foreIndex : backIndex);
        }
        if (page == null) {
            page = viewState.model.getCurrentPageObject();
        }
        if (page != null) {
            updateForeBitmap(event, page);

            final Rect src = new Rect(0, 0, (int) viewState.viewRect.width(), (int) viewState.viewRect.height());
            final RectF dst = new RectF(0, 0, viewState.viewRect.width(), viewState.viewRect.height());

            event.canvas.drawBitmap(foreBitmap.getBitmap(), src, dst, PAINT);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#drawBackground(org.ebookdroid.core.EventDraw)
     */
    @Override
    protected void drawBackground(final EventDraw event) {
    }
}
