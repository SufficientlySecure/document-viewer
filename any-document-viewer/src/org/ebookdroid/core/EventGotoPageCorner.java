package org.ebookdroid.core;

import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.ui.viewer.IView;

import android.graphics.PointF;
import android.graphics.RectF;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;

public class EventGotoPageCorner implements IEvent {

    public static final LogContext LCTX = LogManager.root().lctx("EventGotoPage");

    protected AbstractViewController ctrl;
    protected final ViewState viewState;
    protected DocumentModel model;
    protected int viewIndex;
    protected final float offsetX;
    protected final float offsetY;

    public EventGotoPageCorner(final AbstractViewController ctrl, final float offsetX, final float offsetY) {
        this.viewState = ViewState.get(ctrl);
        this.ctrl = ctrl;
        this.model = viewState.model;
        this.viewIndex = ctrl.getBase().getDocumentModel().getCurrentViewPageIndex();
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    @Override
    public ViewState process() {
        if (model == null) {
            return null;
        }

        final int pageCount = model.getPageCount();
        if (viewIndex < 0 && viewIndex >= pageCount) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Bad page index: " + viewIndex + ", page count: " + pageCount);
            }
            return viewState;
        }

        final Page page = model.getPageObject(viewIndex);
        if (page == null) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("No page found for index: " + viewIndex);
            }
            return viewState;
        }

        final IView view = ctrl.getView();
        final PointF p = calculateScroll(page);
        final int left = Math.round(p.x);
        final int top = Math.round(p.y);

        view.scrollTo(left, top);
        viewState.update();
        return viewState;
    }

    protected PointF calculateScroll(final Page page) {
        final RectF viewRect = ctrl.getView().getViewRect();

        final RectF bounds = page.getBounds(viewState.zoom);
        final float pageCornerX = bounds.left + offsetX * bounds.width();
        final float pageCornerY = bounds.top + offsetY * bounds.height();

        final float targetX = pageCornerX - offsetX * viewRect.width();
        final float targetY = pageCornerY - offsetY * viewRect.height();

        return new PointF(targetX, targetY);
    }

    @Override
    public boolean process(final Page page) {
        return false;
    }

    @Override
    public boolean process(final PageTree nodes) {
        return false;
    }

    @Override
    public boolean process(final PageTree nodes, final PageTreeLevel level) {
        return false;
    }

    @Override
    public boolean process(final PageTreeNode node) {
        return false;
    }
}
