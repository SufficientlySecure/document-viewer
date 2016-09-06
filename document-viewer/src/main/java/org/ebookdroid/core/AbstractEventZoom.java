package org.ebookdroid.core;

import org.ebookdroid.common.settings.SettingsManager;

import android.graphics.PointF;
import android.graphics.RectF;

import java.util.Queue;

import org.emdev.utils.LengthUtils;

public abstract class AbstractEventZoom<E extends AbstractEventZoom<E>> extends AbstractEvent {

    private final Queue<E> eventQueue;

    public float oldZoom;
    public float newZoom;

    public PageTreeLevel oldLevel;
    public PageTreeLevel newLevel;

    public boolean committed;

    public PointF center;

    protected AbstractEventZoom(final Queue<E> eventQueue) {
        this.eventQueue = eventQueue;
    }

    final void init(final AbstractViewController ctrl, final float oldZoom, final float newZoom, final boolean committed, PointF center) {
        this.viewState = ViewState.get(ctrl, newZoom);
        this.ctrl = ctrl;

        this.oldZoom = oldZoom;
        this.newZoom = newZoom;

        this.oldLevel = PageTreeLevel.getLevel(oldZoom);
        this.newLevel = PageTreeLevel.getLevel(newZoom);

        this.committed = committed;
        this.center = center;
    }

    @SuppressWarnings("unchecked")
    final void release() {
        this.ctrl = null;
        this.center = null;
        this.viewState = null;
        this.oldLevel = null;
        this.newLevel = null;
        this.bitmapsToRecycle.clear();
        this.nodesToDecode.clear();
        eventQueue.offer((E) this);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.AbstractEvent#process()
     */
    @Override
    public final ViewState process() {
        try {
            if (!committed) {
                ctrl.getView().invalidateScroll(newZoom, oldZoom, center);
                viewState.update();
            }

            super.process();

            if (!committed) {
                ctrl.redrawView(viewState);
            } else {
                SettingsManager.zoomChanged(viewState.book, newZoom, true);
                ctrl.updatePosition(ctrl.model.getCurrentPageObject(), viewState);
            }
            return viewState;
        } finally {
            release();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IEvent#process(org.ebookdroid.core.ViewState, org.ebookdroid.core.PageTree)
     */
    @Override
    public final boolean process(final PageTree nodes) {
        return process(nodes, newLevel);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.AbstractEvent#calculatePageVisibility(org.ebookdroid.core.ViewState)
     */
    @Override
    protected final void calculatePageVisibility() {
        final int viewIndex = ctrl.model.getCurrentViewPageIndex();
        int firstVisiblePage = viewIndex;
        int lastVisiblePage = viewIndex;

        final Page[] pages = ctrl.model.getPages();
        if (LengthUtils.isEmpty(pages)) {
            return;
        }

        final RectF bounds = new RectF();
        while (firstVisiblePage > 0) {
            final int index = firstVisiblePage - 1;
            if (!ctrl.isPageVisible(pages[index], viewState, bounds)) {
                break;
            }
            firstVisiblePage = index;
        }
        while (lastVisiblePage < pages.length - 1) {
            final int index = lastVisiblePage + 1;
            if (!ctrl.isPageVisible(pages[index], viewState, bounds)) {
                break;
            }
            lastVisiblePage = index;
        }

        viewState.update(firstVisiblePage, lastVisiblePage);
    }
}
