package org.ebookdroid.core;

import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.ui.viewer.IView;

import android.graphics.PointF;
import android.graphics.RectF;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;

public class EventGotoPage implements IEvent {

    public static final LogContext LCTX = LogManager.root().lctx("EventGotoPage");

    protected final boolean centerPage;

    protected AbstractViewController ctrl;
    protected final ViewState viewState;
    protected DocumentModel model;
    protected int viewIndex;
    protected final float offsetX;
    protected final float offsetY;

    public EventGotoPage(final AbstractViewController ctrl, final int viewIndex) {
        this.viewState = ViewState.get(ctrl);
        this.ctrl = ctrl;
        this.model = viewState.model;
        this.centerPage = true;
        this.viewIndex = viewIndex;
        this.offsetX = 0;
        this.offsetY = 0;
    }

    public EventGotoPage(final AbstractViewController ctrl, final int viewIndex, final float offsetX,
            final float offsetY) {
        this.viewState = ViewState.get(ctrl);
        this.ctrl = ctrl;
        this.model = viewState.model;
        this.centerPage = false;
        this.viewIndex = viewIndex;
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

        model.setCurrentPageIndex(page.index);

        final IView view = ctrl.getView();

        final int scrollX = view.getScrollX();
        final int scrollY = view.getScrollY();

        final PointF p = calculateScroll(page, scrollX, scrollY);
        final int left = Math.round(p.x);
        final int top = Math.round(p.y);

        if (isScrollRequired(left, top, scrollX, scrollY)) {
            view.forceFinishScroll();
            view.scrollTo(left, top);
            viewState.update();
            return viewState;
        }
        viewState.release();

        return EventPool.newEventScrollTo(ctrl, viewIndex).process();
    }

    protected PointF calculateScroll(final Page page, final int scrollX, final int scrollY) {
        final RectF viewRect = ctrl.getView().getViewRect();
        final RectF bounds = page.getBounds(viewState.zoom);
        final float width = bounds.width();
        final float height = bounds.height();

        if (centerPage) {
            switch (ctrl.mode) {
                case HORIZONTAL_SCROLL:
                    return new PointF(bounds.left - (viewRect.width() - width) / 2, scrollY);
                case VERTICALL_SCROLL:
                    return new PointF(scrollX, bounds.top - (viewRect.height() - height) / 2);
            }
        }

        return new PointF(bounds.left + offsetX * width, bounds.top + offsetY * height);
    }

    protected boolean isScrollRequired(final int left, final int top, final int scrollX, final int scrollY) {
        switch (ctrl.mode) {
            case HORIZONTAL_SCROLL:
                return left != scrollX;
            case VERTICALL_SCROLL:
                return top != scrollY;
        }
        return true;
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
