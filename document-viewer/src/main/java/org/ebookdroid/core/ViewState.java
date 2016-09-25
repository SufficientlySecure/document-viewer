package org.ebookdroid.core;

import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.common.settings.types.PageType;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.models.DocumentModel.PageIterator;
import org.ebookdroid.ui.viewer.IView;
import org.ebookdroid.ui.viewer.IViewController;

import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ViewState {

    private static final ConcurrentLinkedQueue<ViewState> states = new ConcurrentLinkedQueue<ViewState>();

    private State state;

    public AppSettings app;
    public BookSettings book;
    public IViewController ctrl;
    public DocumentModel model;

    public boolean nightMode;
    public boolean positiveImagesInNightMode;
    public boolean tint;
    public int tintColor;
    public float zoom;
    public PageAlign pageAlign;
    public PagePaint paint;

    public final Pages pages;

    public RectF viewRect;
    public PointF viewBase;

    public static ViewState get(final IViewController dc) {
        return get().init(dc, dc.getBase().getZoomModel().getZoom());
    }

    public static ViewState get(final IViewController dc, final float zoom) {
        return get().init(dc, zoom);
    }

    private static ViewState get() {
        ViewState state = null;
        while (true) {
            state = states.poll();
            if (state == null) {
                state = new ViewState();
                return state;
            }
            if (state.use()) {
                return state;
            }
        }
    }

    private ViewState() {
        pages = new Pages();
        state = State.RELEASED;
        use();
    }

    private synchronized boolean use() {
        if (state == State.RELEASED || state == State.RELEASED_AFTER_DRAW) {
            state = State.USED;
            return true;
        }
        return false;
    }

    public synchronized void release() {
        switch (state) {
            case USED:
                state = State.RELEASED;
                states.add(this);
                return;
            case QUEUED:
            case RELEASED:
            case RELEASED_AFTER_DRAW:
                return;
        }
    }

    public synchronized void addedToDrawQueue() {
        switch (state) {
            case USED:
                state = State.QUEUED;
                return;
            case QUEUED:
            case RELEASED:
            case RELEASED_AFTER_DRAW:
                return;
        }
    }

    public synchronized void releaseAfterDraw() {
        switch (state) {
            case QUEUED:
                state = State.RELEASED_AFTER_DRAW;
                states.add(this);
                return;
            case USED:
            case RELEASED:
            case RELEASED_AFTER_DRAW:
                return;
        }
    }

    private ViewState init(final IViewController dc, final float zoom) {

        this.app = AppSettings.current();
        this.book = dc.getBase().getBookSettings();
        this.ctrl = dc;
        this.model = dc.getBase().getDocumentModel();
        this.nightMode = book != null ? book.nightMode : app.nightMode;
        this.positiveImagesInNightMode = book != null ? book.positiveImagesInNightMode : app.positiveImagesInNightMode;
        this.tint = book != null ? book.tint : app.tint;
        this.tintColor = book != null ? book.tintColor : app.tintColor;
        this.pageAlign = DocumentViewMode.getPageAlign(book);
        this.paint = this.tint ? PagePaint.TintedDay(this.tintColor)
                : (this.nightMode ? PagePaint.Night()
                    : PagePaint.Day());
        this.paint.bitmapPaint.setFilterBitmap(false);

        this.zoom = zoom;

        this.viewRect = new RectF(ctrl.getView().getViewRect());
        this.viewBase = ctrl.getView().getBase(viewRect);
        this.pages.update();

        return this;
    }

    public void update() {
        this.zoom = ctrl.getBase().getZoomModel().getZoom();
        this.viewRect = new RectF(ctrl.getView().getViewRect());
        this.viewBase = ctrl.getView().getBase(viewRect);
        this.pages.update(ctrl.getFirstVisiblePage(), ctrl.getLastVisiblePage());
    }

    public ViewState update(final int firstVisiblePage, final int lastVisiblePage) {
        this.pages.update(firstVisiblePage, lastVisiblePage);
        return this;
    }

    public RectF getBounds(final Page page) {
        return page.getBounds(zoom);
    }

    public void getBounds(final Page page, final RectF target) {
        page.getBounds(zoom, target);
    }

    public final boolean isPageKeptInMemory(final Page page) {
        return pages.firstCached <= page.index.viewIndex && page.index.viewIndex <= pages.lastCached;
    }

    public final boolean isPageVisible(final Page page) {
        return pages.firstVisible <= page.index.viewIndex && page.index.viewIndex <= pages.lastVisible;
    }

    public final boolean isNodeKeptInMemory(final PageTreeNode node, final RectF pageBounds) {
        if (this.zoom < 1.5) {
            return this.isPageKeptInMemory(node.page) || this.isPageVisible(node.page);
        }
        if (this.zoom < 2.5) {
            return this.isPageKeptInMemory(node.page) || this.isNodeVisible(node, pageBounds);
        }
        return this.isNodeVisible(node, pageBounds);
    }

    public final boolean isNodeVisible(final PageTreeNode node, final RectF pageBounds) {
        final RectF tr = node.getTargetRect(pageBounds);
        return isNodeVisible(tr);
    }

    public final boolean isNodeVisible(final RectF tr) {
        return RectF.intersects(viewRect, tr);
    }

    public final PointF getPositionOnPage(final Page page) {
        final PointF pos = new PointF();
        final IView view = ctrl.getView();
        if (view != null) {
            final int left = view.getScrollX();
            final int top = view.getScrollY();
            final RectF cpBounds = getBounds(page);

            pos.x = (left - cpBounds.left) / cpBounds.width();
            pos.y = (top - cpBounds.top) / cpBounds.height();
        }
        return pos;
    }

    public final PointF getPositionOnPage(final Page page, final int x, final int y) {
        final PointF pos = new PointF();
        final IView view = ctrl.getView();
        if (view != null) {
            System.out.println("ViewState.getPositionOnPage(" + x + "," + y + "," + view.getScrollX() + ","
                    + view.getScrollY() + ")");
            final int left = x + view.getScrollX();
            final int top = y + view.getScrollY();
            final RectF cpBounds = getBounds(page);

            pos.x = (left - cpBounds.left) / cpBounds.width();
            pos.y = (top - cpBounds.top) / cpBounds.height();
            final RectF cropping = page.getCropping();
            if (cropping != null) {
                pos.x *= cropping.width();
                pos.x += cropping.left;
                pos.y *= cropping.height();
                pos.y += cropping.top;
            } else {
                if (page.type == PageType.RIGHT_PAGE) {
                    pos.x += 0.5;
                }
            }

        }
        return pos;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();

        pages.toString(buf).append(" ").append("zoom: ").append(zoom);

        return buf.toString();
    }

    public class Pages extends ViewPages {

        private Pages() {
        }

        public void update() {
            update(ctrl.getFirstVisiblePage(), ctrl.getLastVisiblePage());
        }

        public void update(final int firstVisible, final int lastVisible) {
            this.firstVisible = firstVisible;
            this.lastVisible = lastVisible;

            if (model != null) {
                this.currentIndex = ctrl.calculateCurrentPage(ViewState.this, firstVisible, lastVisible);

                final int inMemory = (int) Math.ceil(app.pagesInMemory / 2.0);
                this.firstCached = Math.max(0, this.currentIndex - inMemory);
                this.lastCached = Math.min(this.currentIndex + inMemory, model.getPageCount());
            } else {
                this.currentIndex = firstVisible;
                this.firstCached = firstVisible;
                this.lastCached = lastVisible;
            }
        }

        public PageIterator getVisiblePages() {
            return firstVisible != -1 ? model.getPages(firstVisible, lastVisible + 1) : model.getPages(0);
        }

        public Page getCurrentPage() {
            return model.getPageObject(currentIndex);
        }
    }

    public static enum State {
        USED, QUEUED, RELEASED, RELEASED_AFTER_DRAW;
    }
}
