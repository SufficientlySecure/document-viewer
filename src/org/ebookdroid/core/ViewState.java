package org.ebookdroid.core;

import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.settings.AppSettings;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.settings.books.BookSettings;

import android.graphics.RectF;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.Map;

public class ViewState {

    public final IDocumentViewController ctrl;
    public final IDocumentView view;

    public final int currentIndex;
    public final int firstVisible;
    public final int lastVisible;

    public final int firstCached;
    public final int lastCached;

    public final RectF realRect;
    public final RectF viewRect;

    public final float zoom;

    public final PageAlign pageAlign;
    public final DecodeMode decodeMode;
    public final boolean nightMode;

    public final SparseArray<RectF> pages = new SparseArray<RectF>();

    public final Map<String, Boolean> nodeVisibility = new HashMap<String, Boolean>();

    public final boolean outOfMemoryOccured;

    public ViewState(final PageTreeNode node) {
        this(node.page.base.getDocumentController());
    }

    public ViewState(final IDocumentViewController dc) {
        this(dc, dc.getBase().getZoomModel().getZoom());
    }

    public ViewState(final IDocumentViewController dc, final float zoom) {
        this.ctrl = dc;
        this.view = dc.getView();

        this.firstVisible = dc.getFirstVisiblePage();
        this.lastVisible = dc.getLastVisiblePage();

        this.realRect = new RectF(0, 0, view.getWidth(), view.getHeight());
        this.viewRect = new RectF(view.getViewRect());
        this.zoom = zoom;

        final DocumentModel dm = dc.getBase().getDocumentModel();
        if (dm != null) {
            for (final Page page : dm.getPages(firstVisible, lastVisible + 1)) {
                pages.append(page.index.viewIndex, page.getBounds(zoom));
            }
            this.currentIndex = dc.calculateCurrentPage(this);
            final int inMemory = (int) Math.ceil(SettingsManager.getAppSettings().getPagesInMemory() / 2.0);
            this.firstCached = Math.max(0, this.currentIndex - inMemory);
            this.lastCached = Math.min(this.currentIndex + inMemory, dm.getPageCount());
            this.outOfMemoryOccured = dm.getDecodeService().getMemoryLimit() < Long.MAX_VALUE;
        } else {
            this.currentIndex = 0;
            this.firstCached = 0;
            this.lastCached = 0;
            this.outOfMemoryOccured = false;
        }

        final BookSettings bs = SettingsManager.getBookSettings();
        final AppSettings as = SettingsManager.getAppSettings();

        this.pageAlign = DocumentViewMode.getPageAlign(bs);
        this.decodeMode = as.getDecodeMode();
        this.nightMode = as.getNightMode();
    }

    public ViewState(final ViewState oldState, final IDocumentViewController dc) {
        this.ctrl = dc;
        this.view = dc.getView();

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
        this.lastCached = Math.min(this.currentIndex + inMemory, dc.getBase().getDocumentModel().getPageCount());

        this.pageAlign = oldState.pageAlign;
        this.decodeMode = oldState.decodeMode;
        this.nightMode = oldState.nightMode;
        this.outOfMemoryOccured = oldState.outOfMemoryOccured;
    }

    public ViewState(final ViewState oldState) {
        this.ctrl = oldState.ctrl;
        this.view = oldState.view;

        this.firstVisible = oldState.firstVisible;
        this.lastVisible = oldState.lastVisible;

        this.realRect = oldState.realRect;
        this.viewRect = oldState.viewRect;
        this.zoom = oldState.zoom;

        this.currentIndex = oldState.currentIndex;
        this.firstCached = oldState.firstCached;
        this.lastCached = oldState.lastCached;

        this.pageAlign = oldState.pageAlign;
        this.decodeMode = oldState.decodeMode;
        this.nightMode = oldState.nightMode;
        this.outOfMemoryOccured = oldState.outOfMemoryOccured;
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
        return !outOfMemoryOccured && firstCached <= page.index.viewIndex && page.index.viewIndex <= lastCached;
    }

    public final boolean isPageVisible(final Page page) {
        return firstVisible <= page.index.viewIndex && page.index.viewIndex <= lastVisible;
    }

    public final boolean isNodeKeptInMemory(final PageTreeNode node, final RectF pageBounds) {
        if (this.decodeMode == DecodeMode.NATIVE_RESOLUTION || this.zoom < 1.5) {
            return !outOfMemoryOccured && this.isPageKeptInMemory(node.page) || this.isPageVisible(node.page);
        }
        if (this.zoom < 2.5) {
            return !outOfMemoryOccured && this.isPageKeptInMemory(node.page) || isPageVisible(node.page) && this.isNodeVisible(node, pageBounds);
        }
        return this.isPageVisible(node.page) && this.isNodeVisible(node, pageBounds);
    }

    public final boolean isNodeVisible(final PageTreeNode node, final RectF pageBounds) {
        Boolean res = nodeVisibility.get(node.shortId);
        if (res == null) {
            final RectF tr = node.getTargetRect(this.viewRect, pageBounds);
            res = RectF.intersects(tr, this.viewRect);
            nodeVisibility.put(node.shortId, res);

        }
        return res.booleanValue();
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();

        buf.append("visible: ").append("[").append(firstVisible).append(", ").append(currentIndex).append(", ")
                .append(lastVisible).append("]");
        buf.append(" ");
        buf.append("cached: ").append("[").append(firstCached).append(", ").append(lastCached).append("]");
        buf.append(" ");
        buf.append("zoom: ").append(zoom);

        return buf.toString();
    }

}
