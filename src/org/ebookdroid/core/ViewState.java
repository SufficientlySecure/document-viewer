package org.ebookdroid.core;

import org.ebookdroid.core.settings.SettingsManager;

import android.graphics.RectF;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.Map;

public class ViewState {

    public final int currentIndex;
    public final int firstVisible;
    public final int lastVisible;

    public final int firstCached;
    public final int lastCached;

    public final RectF realRect;
    public final RectF viewRect;

    public final float zoom;

    public boolean lowMemory;
    public boolean nativeResolution;

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

        final int inMemory = (int) Math.ceil(SettingsManager.getAppSettings().getPagesInMemory() / 2.0);
        this.firstCached = Math.max(0, this.currentIndex - inMemory);
        this.lastCached = Math.min(this.currentIndex - inMemory, dc.getBase().getDocumentModel().getPageCount());

        this.lowMemory = SettingsManager.getAppSettings().getLowMemory();
        this.nativeResolution = lowMemory ? false : SettingsManager.getAppSettings().getNativeResolution();
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

        final int inMemory = (int) Math.ceil(SettingsManager.getAppSettings().getPagesInMemory() / 2.0);
        this.firstCached = Math.max(0, this.currentIndex - inMemory);
        this.lastCached = Math.min(this.currentIndex - inMemory, dc.getBase().getDocumentModel().getPageCount());

        this.lowMemory = oldState.lowMemory;
        this.nativeResolution = oldState.nativeResolution;
    }

    public ViewState(final ViewState state) {
        this.firstVisible = state.firstVisible;
        this.lastVisible = state.lastVisible;

        this.realRect = state.realRect;
        this.viewRect = state.viewRect;
        this.zoom = state.zoom;

        this.currentIndex = state.currentIndex;
        this.firstCached = state.firstCached;
        this.lastCached = state.lastCached;

        this.lowMemory = state.lowMemory;
        this.nativeResolution = state.nativeResolution;
    }

    public RectF getBounds(final Page page) {
        RectF bounds = pages.get(page.index.viewIndex);
        if (bounds == null) {
            bounds = page.getBounds(zoom);
            pages.append(page.index.viewIndex, bounds);
        }
        return bounds;
    }

    public final boolean isPageKeptInMemory(final Page page) {
        return firstCached <= page.index.viewIndex && page.index.viewIndex <= lastCached;
    }

    public final boolean isPageVisible(final Page page) {
        return firstVisible <= page.index.viewIndex && page.index.viewIndex <= lastVisible;
    }

    public final boolean isNodeKeptInMemory(final PageTreeNode node, final RectF pageBounds) {
        if (this.nativeResolution || this.zoom < 2) {
            return this.isPageKeptInMemory(node.page) || this.isPageVisible(node.page);
        }
        if (this.zoom < 4) {
            return this.isPageKeptInMemory(node.page) && this.isPageVisible(node.page);
        }
        return this.isPageVisible(node.page) && this.isNodeVisible(node, pageBounds);
    }

    public final boolean isNodeVisible(final PageTreeNode node, final RectF pageBounds) {
        Boolean res = nodeVisibility.get(node.shortId);
        if (res == null) {
            final RectF tr = node.getTargetRect(this.viewRect, pageBounds);
            res = RectF.intersects(tr, this.realRect);
            nodeVisibility.put(node.shortId, res);
        }
        return res.booleanValue();
    }
}
