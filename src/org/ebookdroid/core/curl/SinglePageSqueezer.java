package org.ebookdroid.core.curl;

import org.ebookdroid.core.EventDraw;
import org.ebookdroid.core.EventGLDraw;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.SinglePageController;

import android.graphics.Canvas;
import android.graphics.Rect;
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
    protected void drawForeground(final EventDraw event) {
        Page page = event.viewState.model.getPageObject(foreIndex);
        if (page == null) {
            page = event.viewState.model.getCurrentPageObject();
        }
        if (page != null) {
            updateForeBitmap(event, page);

            final Canvas canvas = event.canvas;
            final RectF viewRect = event.viewState.viewRect;

            final Rect src = new Rect(0, 0, (int) viewRect.width(), (int) viewRect.height());
            final RectF dst = new RectF(0, 0, viewRect.width() - mA.x, viewRect.height());

            foreBitmap.draw(canvas, src, dst, PAINT);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#drawBackground(org.ebookdroid.core.EventDraw)
     */
    @Override
    protected void drawBackground(final EventDraw event) {
        final Page page = event.viewState.model.getPageObject(backIndex);
        if (page != null) {
            updateBackBitmap(event, page);

            final Canvas canvas = event.canvas;
            final RectF viewRect = event.viewState.viewRect;

            final Rect src = new Rect(0, 0, (int) viewRect.width(), (int) viewRect.height());
            final RectF dst = new RectF(viewRect.width() - mA.x, 0, viewRect.width(), viewRect.height());

            backBitmap.draw(canvas, src, dst, PAINT);
        }

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
