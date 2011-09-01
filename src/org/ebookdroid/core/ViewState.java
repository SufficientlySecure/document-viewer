package org.ebookdroid.core;

import android.graphics.RectF;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.Map;

public class ViewState {

    public final int currentIndex;
    public final int firstVisible;
    public final int lastVisible;

    public final RectF realRect;
    public final RectF viewRect;

    public final float zoom;

    public final SparseArray<RectF> pages = new SparseArray<RectF>();
    public final Map<String, Boolean> nodeVisibility = new HashMap<String, Boolean>();

    public ViewState(final PageTreeNode node) {
        this(node.page.base.getDocumentController());
    }

    public ViewState(final IDocumentViewController dc) {
        this(dc, dc.getBase().getZoomModel().getZoom());
    }

    public ViewState(final IDocumentViewController dc, final float zoom) {
        this.firstVisible = dc.getFirstVisiblePage();
        this.lastVisible = dc.getLastVisiblePage();

        this.realRect = new RectF(0, 0, dc.getView().getWidth(), dc.getView().getHeight());
        this.viewRect = new RectF(dc.getViewRect());
        this.zoom = zoom;

        for (final Page page : dc.getBase().getDocumentModel().getPages(firstVisible, lastVisible + 1)) {
            pages.append(page.index.viewIndex, page.getBounds(zoom));
        }

        this.currentIndex = dc.calculateCurrentPage(this);
    }

    public ViewState(final ViewState oldState, final IDocumentViewController dc) {
        this.firstVisible = dc.getFirstVisiblePage();
        this.lastVisible = dc.getLastVisiblePage();

        this.realRect = oldState.realRect;
        this.viewRect = oldState.viewRect;
        this.zoom = oldState.zoom;

        final int min = Math.min(firstVisible, oldState.firstVisible);
        final int max = Math.max(lastVisible, oldState.lastVisible);
        for (final Page page : dc.getBase().getDocumentModel().getPages(min, max + 1)) {
            pages.append(page.index.viewIndex, page.getBounds(zoom));
        }

        this.currentIndex = dc.calculateCurrentPage(this);
    }

    public RectF getBounds(final Page page) {
        RectF bounds = pages.get(page.index.viewIndex);
        if (bounds == null) {
            bounds = page.getBounds(zoom);
            pages.append(page.index.viewIndex, bounds);
        }
        return bounds;
    }

    public final boolean isPageVisible(final Page page) {
        return firstVisible <= page.index.viewIndex && page.index.viewIndex <= lastVisible;
    }
}
