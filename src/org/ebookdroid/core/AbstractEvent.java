package org.ebookdroid.core;

import org.ebookdroid.common.bitmaps.GLBitmaps;
import org.ebookdroid.common.bitmaps.ByteBufferManager;

import android.graphics.RectF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;

public abstract class AbstractEvent implements IEvent {

    public final LogContext LCTX = LogManager.root().lctx(getClass().getSimpleName(), false);

    protected final List<PageTreeNode> nodesToDecode = new ArrayList<PageTreeNode>();
    protected final List<GLBitmaps> bitmapsToRecycle = new ArrayList<GLBitmaps>();

    public AbstractViewController ctrl;
    protected ViewState viewState;

    protected AbstractEvent() {
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IEvent#process()
     */
    @Override
    public ViewState process() {
        calculatePageVisibility();

        ctrl.firstVisiblePage = viewState.pages.firstVisible;
        ctrl.lastVisiblePage = viewState.pages.lastVisible;

        for (final Page page : ctrl.model.getPages()) {
            process(page);
        }

        ByteBufferManager.release(bitmapsToRecycle);

        if (!nodesToDecode.isEmpty()) {
            ctrl.base.getDecodingProgressModel().increase(nodesToDecode.size());
            decodePageTreeNodes(viewState, nodesToDecode);
            if (LCTX.isDebugEnabled()) {
                LCTX.d(viewState + " => " + nodesToDecode.size());
            }
        }

        return viewState;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IEvent#process(org.ebookdroid.core.ViewState, org.ebookdroid.core.Page)
     */
    @Override
    public final boolean process(final Page page) {
        if (page.recycled) {
            return false;
        }
        if (viewState.isPageKeptInMemory(page) || viewState.isPageVisible(page)) {
            return process(page.nodes);
        }

        recyclePage(viewState, page);
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IEvent#process(org.ebookdroid.core.ViewState, org.ebookdroid.core.PageTree,
     *      org.ebookdroid.core.PageTreeLevel)
     */
    @Override
    public boolean process(final PageTree nodes, final PageTreeLevel level) {
        return nodes.process(this, level, true);
    }

    protected void calculatePageVisibility() {
        int firstVisiblePage = -1;
        int lastVisiblePage = -1;
        final RectF bounds = new RectF();
        for (final Page page : ctrl.model.getPages()) {
            if (ctrl.isPageVisible(page, viewState, bounds)) {
                if (firstVisiblePage == -1) {
                    firstVisiblePage = page.index.viewIndex;
                }
                lastVisiblePage = page.index.viewIndex;
            } else if (firstVisiblePage != -1) {
                break;
            }
        }
        viewState.update(firstVisiblePage, lastVisiblePage);
    }

    protected final void decodePageTreeNodes(final ViewState viewState, final List<PageTreeNode> nodesToDecode) {
        final PageTreeNode best = Collections.min(nodesToDecode, new PageTreeNodeComparator(viewState));
        final DecodeService ds = ctrl.getBase().getDecodeService();

        if (ds != null) {
            ds.decodePage(viewState, best);

            for (final PageTreeNode node : nodesToDecode) {
                if (node != best) {
                    ds.decodePage(viewState, node);
                }
            }
        }
    }

    protected final void recyclePage(final ViewState viewState, final Page page) {
        final int oldSize = bitmapsToRecycle.size();
        final boolean res = page.nodes.recycleAll(bitmapsToRecycle, true);
        if (res) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Recycle page " + page.index + " " + viewState.pages + " = " + (bitmapsToRecycle.size() - oldSize));
            }
        }

    }
}
