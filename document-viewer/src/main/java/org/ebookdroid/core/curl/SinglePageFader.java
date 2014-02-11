package org.ebookdroid.core.curl;

import org.ebookdroid.core.EventGLDraw;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.SinglePageController;

import android.graphics.RectF;

import org.emdev.utils.MathUtils;

public class SinglePageFader extends AbstractPageSlider {

    public SinglePageFader(final SinglePageController singlePageDocumentView) {
        super(PageAnimationType.FADER, singlePageDocumentView);
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
            event.process(page);
        }
    }

    @Override
    protected void drawBackground(final EventGLDraw event) {
        final Page page = event.viewState.model.getPageObject(backIndex);
        if (page != null) {
            final RectF viewRect = event.viewState.viewRect;
            event.canvas.save();
            event.canvas.setAlpha(MathUtils.adjust(mA.x / viewRect.width(), 0f, 1f));
            event.process(page);
            event.canvas.restore();
        }
    }

}
