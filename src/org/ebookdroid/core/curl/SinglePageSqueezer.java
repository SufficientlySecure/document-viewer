package org.ebookdroid.core.curl;

import org.ebookdroid.core.EventGLDraw;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.SinglePageController;

import android.graphics.RectF;

public class SinglePageSqueezer extends AbstractPageSlider {

    public SinglePageSqueezer(final SinglePageController singlePageDocumentView) {
        super(PageAnimationType.SQUEEZER, singlePageDocumentView);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#drawForeground(org.ebookdroid.core.EventDraw)
     */
    @Override
    protected void drawForeground(final EventGLDraw event) {
        Page page = event.viewState.model.getPageObject(foreIndex);
        if (page == null) {
            page = event.viewState.model.getCurrentPageObject();
        }
        if (page != null) {
            final RectF viewRect = event.viewState.viewRect;
            event.canvas.save();
            event.canvas.translate(-mA.x, 0);
            event.canvas.scale((viewRect.width() - mA.x) / viewRect.width(), 1, 1);
            event.process(page);
            event.canvas.restore();
        }
    }

    @Override
    protected void drawBackground(final EventGLDraw event) {
        Page page = event.viewState.model.getPageObject(backIndex);
        if (page == null) {
            page = event.viewState.model.getCurrentPageObject();
        }
        if (page != null) {
            final RectF viewRect = event.viewState.viewRect;
            event.canvas.save();
            event.canvas.translate(-mA.x + event.viewState.viewRect.width(), 0);
            event.canvas.scale((mA.x) / viewRect.width(), 1, 1);
            event.process(page);
            event.canvas.restore();
        }
    }

}
