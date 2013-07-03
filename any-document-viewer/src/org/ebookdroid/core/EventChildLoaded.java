package org.ebookdroid.core;

import org.ebookdroid.common.bitmaps.ByteBufferManager;
import org.ebookdroid.common.bitmaps.GLBitmaps;

import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class EventChildLoaded extends AbstractEvent {

    private final Queue<EventChildLoaded> eventQueue;

    public Page page;
    public PageTree nodes;
    public PageTreeNode child;

    public EventChildLoaded(final Queue<EventChildLoaded> eventQueue) {
        this.eventQueue = eventQueue;
    }

    final void init(final AbstractViewController ctrl, final PageTreeNode child) {
        this.viewState = ViewState.get(ctrl);
        this.ctrl = ctrl;
        this.page = child.page;
        this.nodes = page.nodes;
        this.child = child;
    }

    final void release() {
        this.ctrl = null;
        this.viewState = null;
        this.child = null;
        this.nodes = null;
        this.page = null;
        this.bitmapsToRecycle.clear();
        this.nodesToDecode.clear();
        eventQueue.offer(this);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.AbstractEvent#process()
     */
    @Override
    public final ViewState process() {
        try {
            if (ctrl == null || viewState.book == null) {
                return null;
            }

            final RectF bounds = viewState.getBounds(page);
            final PageTreeNode parent = child.parent;
            if (parent != null) {
                recycleParent(parent, bounds);
            }
            recycleChildren();

            ctrl.pageUpdated(viewState, page);

            if (viewState.isPageVisible(page) && viewState.isNodeVisible(child, viewState.getBounds(page))) {
                ctrl.redrawView(viewState);
            }

            return viewState;
        } finally {
            release();
        }
    }

    protected void recycleParent(final PageTreeNode parent, final RectF bounds) {
        final boolean hiddenByChildren = nodes.isHiddenByChildren(parent, viewState, bounds);

        // if (LCTX.isDebugEnabled()) {
        // LCTX.d("Node " + parent.fullId + " is: " + (hiddenByChildren ? "" : "not") + " hidden by children");
        // }

        if (!viewState.isNodeVisible(parent, bounds) || hiddenByChildren) {
            final List<GLBitmaps> bitmapsToRecycle = new ArrayList<GLBitmaps>();
            final boolean res = nodes.recycleParents(child, bitmapsToRecycle);
            ByteBufferManager.release(bitmapsToRecycle);

            if (res) {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("Recycle parent nodes for: " + child.fullId + " " + bitmapsToRecycle.size());
                }
            }
        }
    }

    protected void recycleChildren() {
        final boolean res = nodes.recycleChildren(child, bitmapsToRecycle);
        ByteBufferManager.release(bitmapsToRecycle);

        if (res) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Recycle children nodes for: " + child.fullId + " " + bitmapsToRecycle.size());
            }
        }
    }

    @Override
    public boolean process(final PageTree nodes) {
        return false;
    }

    @Override
    public boolean process(final PageTreeNode node) {
        return false;
    }
}
