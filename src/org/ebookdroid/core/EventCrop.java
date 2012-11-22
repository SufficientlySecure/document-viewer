package org.ebookdroid.core;

import org.ebookdroid.common.bitmaps.GLBitmaps;
import org.ebookdroid.common.bitmaps.ByteBufferManager;
import org.ebookdroid.common.settings.types.PageType;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.models.DocumentModel.PageIterator;
import org.ebookdroid.ui.viewer.IViewController;
import org.ebookdroid.ui.viewer.IViewController.InvalidateSizeReason;

import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;

public class EventCrop extends AbstractEvent {

    private static final RectF FULL_PAGE_CROPPING = new RectF(0f, 0f, 1f, 1f);

    protected final PageTreeLevel level;
    protected final InvalidateSizeReason reason = InvalidateSizeReason.PAGE_LOADED;

    protected final List<Page> pages = new ArrayList<Page>();
    protected final boolean commit;

    protected final RectF[] croppings;
    protected boolean processAll;

    public EventCrop(final IViewController ctrl) {
        this(ctrl, FULL_PAGE_CROPPING, false);
    }

    public EventCrop(final IViewController ctrl, final RectF cropping, final boolean commit) {
        this.viewState = ViewState.get(ctrl);
        this.ctrl = (AbstractViewController) ctrl;
        this.level = PageTreeLevel.getLevel(viewState.zoom);
        this.commit = commit;

        final PageType[] types = PageType.values();
        this.croppings = new RectF[types.length];

        for (final PageType type : types) {
            if (cropping != null) {
                final RectF actual = this.croppings[type.ordinal()] = new RectF(type.getInitialRect());
                final float irw = actual.width();
                actual.left += cropping.left * irw;
                actual.right -= (1 - cropping.right) * irw;
                actual.top += cropping.top;
                actual.bottom -= (1 - cropping.bottom);
            }
        }
    }

    public EventCrop add(final Page page) {
        pages.add(page);
        return this;
    }

    public EventCrop addEvenOdd(final Page page, final boolean eq) {
        final int evenOdd = (page.index.viewIndex + (eq ? 0 : 1)) % 2;
        final PageIterator nextPages = ctrl.getBase().getDocumentModel().getPages(page.index.viewIndex);
        try {
            for (final Page p : nextPages) {
                if (evenOdd == (p.index.viewIndex % 2)) {
                    pages.add(p);
                }
            }
        } finally {
            nextPages.release();
        }

        return this;
    }

    public EventCrop addAll() {
        this.processAll = true;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.AbstractEvent#process()
     */
    @Override
    public ViewState process() {
        try {
            final List<GLBitmaps> bitmapsToRecycle = new ArrayList<GLBitmaps>();

            if (processAll) {
                for (final Page page : ctrl.getBase().getDocumentModel().getPages()) {
                    page.nodes.recycleAll(bitmapsToRecycle, true);
                    page.nodes.root.setManualCropping(croppings[page.type.ordinal()], commit);
                }
            } else {
                for (final Page page : pages) {
                    page.nodes.recycleAll(bitmapsToRecycle, true);
                    page.nodes.root.setManualCropping(croppings[page.type.ordinal()], commit);
                }
            }

            final DocumentModel dm = ctrl.getBase().getDocumentModel();
            dm.saveDocumentInfo();

            ByteBufferManager.release(bitmapsToRecycle);

            ctrl.invalidatePageSizes(reason, null);
            ctrl.invalidateScroll();
            viewState.update();

            return super.process();

        } finally {
            pages.clear();
            processAll = false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IEvent#process(org.ebookdroid.core.ViewState, org.ebookdroid.core.PageTree)
     */
    @Override
    public boolean process(final PageTree nodes) {
        return process(nodes, level);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IEvent#process(org.ebookdroid.core.ViewState, org.ebookdroid.core.PageTreeNode)
     */
    @Override
    public boolean process(final PageTreeNode node) {

        final RectF pageBounds = viewState.getBounds(node.page);

        if (!viewState.isNodeKeptInMemory(node, pageBounds)) {
            node.recycle(bitmapsToRecycle);
            return false;
        }

        if (!node.holder.hasBitmaps()) {
            node.decodePageTreeNode(nodesToDecode, viewState);
        }

        return true;
    }
}
