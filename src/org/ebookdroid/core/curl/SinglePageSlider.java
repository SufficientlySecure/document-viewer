package org.ebookdroid.core.curl;

import org.ebookdroid.core.EventGLDraw;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.SinglePageController;

public class SinglePageSlider extends AbstractPageSlider {

    public SinglePageSlider(final SinglePageController singlePageDocumentView) {
        super(PageAnimationType.SLIDER, singlePageDocumentView);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#drawForeground(org.ebookdroid.core.EventGLDraw)
     */
    @Override
    protected void drawForeground(final EventGLDraw event) {
        Page page = event.viewState.model.getPageObject(foreIndex);
        if (page == null) {
            page = event.viewState.model.getCurrentPageObject();
        }
        if (page != null) {
            event.canvas.save();
            event.canvas.translate(-mA.x, 0);
            event.process(page);
            event.canvas.restore();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#drawBackground(org.ebookdroid.core.EventDraw)
     */
    @Override
    protected void drawBackground(final EventGLDraw event) {
        final Page page = event.viewState.model.getPageObject(backIndex);
        if (page != null) {
            event.canvas.save();
            event.canvas.translate(- mA.x + event.viewState.viewRect.width(), 0);
            event.process(page);
            event.canvas.restore();
        }
    }

}
