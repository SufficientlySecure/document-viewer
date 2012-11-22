package org.ebookdroid.core.curl;

import org.ebookdroid.core.EventGLDraw;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.SinglePageController;
import org.ebookdroid.core.ViewState;

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
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#drawForeground(org.ebookdroid.core.EventGLDraw)
     */
    @Override
    protected void drawForeground(final EventGLDraw event) {
        final ViewState viewState = event.viewState;
        Page page = null;
        if (bFlipping) {
            page = viewState.model.getPageObject(!bFlipRight ? foreIndex : backIndex);
        }
        if (page == null) {
            page = viewState.model.getCurrentPageObject();
        }
        if (page != null) {
            event.process(page);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#drawBackground(org.ebookdroid.core.EventGLDraw)
     */
    @Override
    protected void drawBackground(final EventGLDraw event) {
    }

}
