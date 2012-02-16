package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.core.IViewerActivity.IBookLoadTask;
import org.ebookdroid.core.actions.AbstractComponentController;
import org.ebookdroid.core.actions.ActionEx;
import org.ebookdroid.core.actions.ActionMethod;
import org.ebookdroid.core.actions.ActionMethodDef;
import org.ebookdroid.core.actions.ActionTarget;
import org.ebookdroid.core.actions.params.Constant;
import org.ebookdroid.core.bitmaps.BitmapManager;
import org.ebookdroid.core.bitmaps.Bitmaps;
import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.settings.books.BookSettings;
import org.ebookdroid.core.touch.DefaultGestureDetector;
import org.ebookdroid.core.touch.IGestureDetector;
import org.ebookdroid.core.touch.IMultiTouchZoom;
import org.ebookdroid.core.touch.TouchManager;

import android.graphics.Rect;
import android.graphics.RectF;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@ActionTarget(
// action list
actions = {
        // actions
        @ActionMethodDef(id = R.id.actions_verticalConfigScrollUp, method = "verticalConfigScroll"),
        @ActionMethodDef(id = R.id.actions_verticalConfigScrollDown, method = "verticalConfigScroll")
// no more
})
public abstract class AbstractDocumentView extends AbstractComponentController<IDocumentView> implements
        IDocumentViewController {

    protected static final LogContext LCTX = LogContext.ROOT.lctx("View", true);

    public static final int DOUBLE_TAP_TIME = 500;

    protected final IViewerActivity base;

    protected final IDocumentView view;

    protected boolean isInitialized = false;

    protected boolean isShown = false;

    protected final AtomicBoolean inZoom = new AtomicBoolean();

    protected final PageIndex pageToGo;

    protected int firstVisiblePage;

    protected int lastVisiblePage;

    protected float initialZoom;

    protected boolean layoutLocked;

    private List<IGestureDetector> detectors;

    public AbstractDocumentView(final IViewerActivity baseActivity) {
        super(baseActivity.getActivity(), baseActivity.getActionController(), baseActivity.getView());

        this.base = baseActivity;
        this.view = base.getView();

        this.firstVisiblePage = -1;
        this.lastVisiblePage = -1;

        this.pageToGo = SettingsManager.getBookSettings().getCurrentPage();

        createAction(R.id.actions_verticalConfigScrollUp, new Constant("direction", -1));
        createAction(R.id.actions_verticalConfigScrollDown, new Constant("direction", +1));
    }

    protected List<IGestureDetector> getGestureDetectors() {
        if (detectors == null) {
            detectors = initGestureDetectors(new ArrayList<IGestureDetector>(4));
        }
        return detectors;
    }

    protected List<IGestureDetector> initGestureDetectors(final List<IGestureDetector> list) {
        list.add(IMultiTouchZoom.Factory.createImpl(base.getMultiTouchListener()));
        list.add(new DefaultGestureDetector(base.getContext(), new GestureListener()));
        return list;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#getView()
     */
    @Override
    public final IDocumentView getView() {
        return view;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#getBase()
     */
    @Override
    public final IViewerActivity getBase() {
        return base;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#init(org.ebookdroid.core.IViewerActivity.IBookLoadTask)
     */
    @Override
    public final void init(final IBookLoadTask task) {
        if (!isInitialized) {
            try {
                getBase().getDocumentModel().initPages(base, task);
            } finally {
                isInitialized = true;
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#show()
     */
    @Override
    public final void show() {
        if (isInitialized && !isShown) {
            isShown = true;
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Showing view content...");
            }

            invalidatePageSizes(InvalidateSizeReason.INIT, null);

            final BookSettings bs = SettingsManager.getBookSettings();
            final Page page = pageToGo.getActualPage(base.getDocumentModel(), bs);
            final int toPage = page != null ? page.index.viewIndex : 0;

            updatePageVisibility(toPage, 0, base.getZoomModel().getZoom());

            goToPageImpl(toPage, bs.offsetX, bs.offsetY);

        } else {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("View is not initialized yet");
            }
        }
    }

    protected abstract void goToPageImpl(final int toPage);

    protected void updatePosition(final DocumentModel dm, final Page page, final ViewState viewState) {
        final int left = view.getScrollX();
        final int top = view.getScrollY();

        final RectF cpBounds = viewState.getBounds(page);
        final float offsetX = (left - cpBounds.left) / cpBounds.width();
        final float offsetY = (top - cpBounds.top) / cpBounds.height();
        // if (LCTX.isDebugEnabled()) {
        // LCTX.d("Position into page: " + page.index.viewIndex + ", " + offsetX + ", " + offsetY);
        // }
        SettingsManager.positionChanged(offsetX, offsetY);
    }

    protected void goToPageImpl(final int toPage, final float offsetX, final float offsetY) {
        final DocumentModel dm = getBase().getDocumentModel();
        final int pageCount = dm.getPageCount();
        if (toPage >= 0 && toPage < pageCount) {
            final Page page = dm.getPageObject(toPage);
            if (page != null) {
                dm.setCurrentPageIndex(page.index);
                final RectF bounds = page.getBounds(getBase().getZoomModel().getZoom());
                final float left = bounds.left + offsetX * bounds.width();
                final float top = bounds.top + offsetY * bounds.height();
                // if (LCTX.isDebugEnabled()) {
                // LCTX.d("goToPageImpl(): Scroll to: " + page.index.viewIndex + left + ", " + top);
                // }
                view.scrollTo((int) left, (int) top);
            } else {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("goToPageImpl(): No page found for index: " + toPage);
                }
            }
        } else {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("goToPageImpl(): Bad page index: " + toPage + ", page count: " + pageCount);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#onScrollChanged(int, int)
     */
    @Override
    public void onScrollChanged(final int newPage, final int direction) {
        if (inZoom.get()) {
            return;
        }

        final Runnable r = new Runnable() {

            @Override
            public void run() {
                final ViewState viewState = updatePageVisibility(newPage, direction, getBase().getZoomModel().getZoom());
                final DocumentModel dm = getBase().getDocumentModel();
                final Page page = dm.getPageObject(viewState.currentIndex);
                if (page != null) {
                    dm.setCurrentPageIndex(page.index);
                    updatePosition(dm, page, viewState);
                    view.redrawView(viewState);
                }
            }
        };
        base.getActivity().runOnUiThread(r);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#updatePageVisibility(int, int, float)
     */
    @Override
    public final ViewState updatePageVisibility(final int newPage, final int direction, final float zoom) {
        final ViewState viewState = calculatePageVisibility(newPage, direction, zoom);

        final List<PageTreeNode> nodesToDecode = new ArrayList<PageTreeNode>();
        final List<Bitmaps> bitmapsToRecycle = new ArrayList<Bitmaps>();

        for (final Page page : getBase().getDocumentModel().getPages()) {
            page.onPositionChanged(viewState, nodesToDecode, bitmapsToRecycle);
        }

        BitmapManager.release(bitmapsToRecycle);

        if (!nodesToDecode.isEmpty()) {
            decodePageTreeNodes(viewState, nodesToDecode);
            if (LCTX.isDebugEnabled()) {
                LCTX.d("updatePageVisibility: " + viewState + " => " + nodesToDecode.size());
            }
        }

        return viewState;
    }

    protected final void decodePageTreeNodes(final ViewState viewState, final List<PageTreeNode> nodesToDecode) {
        final PageTreeNode best = Collections.min(nodesToDecode, new PageTreeNodeComparator(viewState));
        final DecodeService ds = base.getDecodeService();
        if (ds != null) {
            ds.decodePage(viewState, best, best.croppedBounds != null ? best.croppedBounds : best.pageSliceBounds);

            for (final PageTreeNode node : nodesToDecode) {
                if (node != best) {
                    ds.decodePage(viewState, node, node.croppedBounds != null ? node.croppedBounds
                            : node.pageSliceBounds);
                }
            }
        }
    }

    protected final ViewState calculatePageVisibility(final int newPage, final int direction, final float zoom) {
        final Page[] pages = getBase().getDocumentModel().getPages();
        final ViewState initial = new ViewState(this, zoom);

        if (newPage != -1) {
            firstVisiblePage = newPage;
            lastVisiblePage = newPage;
            while (firstVisiblePage > 0) {
                final int index = firstVisiblePage - 1;
                if (!isPageVisibleImpl(pages[index], initial)) {
                    break;
                }
                firstVisiblePage = index;
            }
            while (lastVisiblePage < pages.length - 1) {
                final int index = lastVisiblePage + 1;
                if (!isPageVisibleImpl(pages[index], initial)) {
                    break;
                }
                lastVisiblePage = index;
            }
        } else if (direction < 0 && lastVisiblePage != -1) {
            for (int i = lastVisiblePage; i >= 0; i--) {
                if (!isPageVisibleImpl(pages[i], initial)) {
                    continue;
                } else {
                    lastVisiblePage = i;
                    break;
                }
            }
            firstVisiblePage = lastVisiblePage;
            while (firstVisiblePage > 0) {
                final int index = firstVisiblePage - 1;
                if (!isPageVisibleImpl(pages[index], initial)) {
                    break;
                }
                firstVisiblePage = index;
            }

        } else if (direction > 0 && firstVisiblePage != -1) {
            for (int i = firstVisiblePage; i < pages.length; i++) {
                if (!isPageVisibleImpl(pages[i], initial)) {
                    continue;
                } else {
                    firstVisiblePage = i;
                    break;
                }
            }
            lastVisiblePage = firstVisiblePage;
            while (lastVisiblePage < pages.length - 1) {
                final int index = lastVisiblePage + 1;
                if (!isPageVisibleImpl(pages[index], initial)) {
                    break;
                }
                lastVisiblePage = index;
            }
        } else {
            firstVisiblePage = -1;
            lastVisiblePage = 1;
            for (final Page page : getBase().getDocumentModel().getPages()) {
                if (isPageVisibleImpl(page, initial)) {
                    if (firstVisiblePage == -1) {
                        firstVisiblePage = page.index.viewIndex;
                    }
                    lastVisiblePage = page.index.viewIndex;
                } else if (firstVisiblePage != -1) {
                    break;
                }
            }
        }

        return new ViewState(initial, this);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.events.ZoomListener#commitZoom()
     */
    @Override
    public final void commitZoom() {
        if (!isShown) {
            return;
        }

        LCTX.d("commitZoom()");
        inZoom.set(false);
        final float newZoom = base.getZoomModel().getZoom();
        SettingsManager.zoomChanged(newZoom, true);
        onZoomChanged(newZoom, true);
        initialZoom = newZoom;
    }

    protected final ViewState onZoomChanged(final float newZoom, final boolean committed) {
        final DocumentModel dm = base.getDocumentModel();
        final ViewState newState = calculatePageVisibility(dm.getCurrentViewPageIndex(), 0, newZoom);

        final List<PageTreeNode> nodesToDecode = new ArrayList<PageTreeNode>();
        final List<Bitmaps> bitmapsToRecycle = new ArrayList<Bitmaps>();

        for (final Page page : getBase().getDocumentModel().getPages()) {
            page.onZoomChanged(initialZoom, newState, committed, nodesToDecode, bitmapsToRecycle);
        }
        BitmapManager.release(bitmapsToRecycle);

        if (!nodesToDecode.isEmpty()) {
            decodePageTreeNodes(newState, nodesToDecode);
        }

        if (LCTX.isDebugEnabled()) {
            LCTX.d("onZoomChanged: " + committed + ", " + newState + " => " + nodesToDecode.size());
        }
        updatePosition(dm, dm.getCurrentPageObject(), newState);
        return newState;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#updateMemorySettings()
     */
    @Override
    public final void updateMemorySettings() {
        final ViewState viewState = new ViewState(this);

        final List<PageTreeNode> nodesToDecode = new ArrayList<PageTreeNode>();
        final List<Bitmaps> bitmapsToRecycle = new ArrayList<Bitmaps>();

        for (final Page page : getBase().getDocumentModel().getPages()) {
            page.onZoomChanged(0, viewState, true, nodesToDecode, bitmapsToRecycle);
        }
        BitmapManager.release(bitmapsToRecycle);

        if (!nodesToDecode.isEmpty()) {
            decodePageTreeNodes(viewState, nodesToDecode);
            if (LCTX.isDebugEnabled()) {
                LCTX.d("updateMemorySettings: " + viewState + " => " + nodesToDecode.size());
            }
        }

    }

    public final ViewState invalidatePages(final ViewState oldState, final Page... pages) {
        final ViewState viewState = calculatePageVisibility(pages[0].index.viewIndex, 0, oldState.zoom);

        final List<PageTreeNode> nodesToDecode = new ArrayList<PageTreeNode>();
        final List<Bitmaps> bitmapsToRecycle = new ArrayList<Bitmaps>();

        for (final Page page : pages) {
            page.onPositionChanged(viewState, nodesToDecode, bitmapsToRecycle);
        }
        BitmapManager.release(bitmapsToRecycle);

        if (!nodesToDecode.isEmpty()) {
            decodePageTreeNodes(viewState, nodesToDecode);
            if (LCTX.isDebugEnabled()) {
                LCTX.d("invalidatePages: " + viewState + " => " + nodesToDecode.size());
            }
        }

        return viewState;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#goToPage(int)
     */
    @Override
    public final void goToPage(final int toPage) {
        if (isShown) {
            goToPageImpl(toPage);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.events.ZoomListener#zoomChanged(float, float)
     */
    @Override
    public final void zoomChanged(final float newZoom, final float oldZoom) {
        if (!isShown) {
            return;
        }

        if (LCTX.isDebugEnabled()) {
            LCTX.d("zoomChanged(" + newZoom + ", " + oldZoom + ")");
        }

        try {
            if (inZoom.compareAndSet(false, true)) {
                initialZoom = oldZoom;
            }

            invalidatePageSizes(InvalidateSizeReason.ZOOM, null);

            view.invalidateScroll(newZoom, oldZoom);

            view.redrawView(onZoomChanged(newZoom, false));
        } catch (final Throwable th) {
            LCTX.e("Unexpected error: ", th);
        }
    }

    public int getScrollX() {
        return view.getScrollX();
    }

    public int getWidth() {
        return view.getWidth();
    }

    public int getScrollY() {
        return view.getScrollY();
    }

    public int getHeight() {
        return view.getHeight();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#dispatchKeyEvent(android.view.KeyEvent)
     */
    @Override
    public boolean dispatchKeyEvent(final KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_VOLUME_UP:
                    verticalConfigScroll(-1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    verticalConfigScroll(1);
                    return true;
            }
        }
        // Avoid sound
        if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                return true;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#onTouchEvent(android.view.MotionEvent)
     */
    @Override
    public final boolean onTouchEvent(final MotionEvent ev) {
        try {
            Thread.sleep(16);
        } catch (final InterruptedException e) {
            Thread.interrupted();
        }

        for (final IGestureDetector d : getGestureDetectors()) {
            if (d.enabled() && d.onTouchEvent(ev)) {
                return true;
            }
        }
        return false;

    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#onLayoutChanged(boolean, boolean, android.graphics.Rect,
     *      android.graphics.Rect)
     */
    @Override
    public boolean onLayoutChanged(final boolean layoutChanged, final boolean layoutLocked, final Rect oldLaout,
            final Rect newLayout) {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onLayoutChanged(" + layoutChanged + ", " + layoutLocked + "," + oldLaout + ", " + newLayout + ")");
        }
        if (layoutChanged && !layoutLocked) {
            if (isShown) {
                final List<Bitmaps> bitmapsToRecycle = new ArrayList<Bitmaps>();
                for (final Page page : base.getDocumentModel().getPages()) {
                    page.nodes.recycleAll(bitmapsToRecycle, true);
                }
                BitmapManager.release(bitmapsToRecycle);

                invalidatePageSizes(InvalidateSizeReason.LAYOUT, null);
                invalidateScroll();
                final float oldZoom = base.getZoomModel().getZoom();
                initialZoom = 0;
                view.redrawView(onZoomChanged(oldZoom, true));
                return true;
            }
        }
        return false;
    }

    @Override
    public void toggleNightMode(boolean nightMode) {
        final List<Bitmaps> bitmapsToRecycle = new ArrayList<Bitmaps>();
        for (final Page page : base.getDocumentModel().getPages()) {
            page.nodes.recycleAll(bitmapsToRecycle, true);
        }
        BitmapManager.release(bitmapsToRecycle);

        final float oldZoom = base.getZoomModel().getZoom();
        initialZoom = 0;
        view.redrawView(onZoomChanged(oldZoom, true));
    }

    protected final void invalidateScroll() {
        if (!isShown) {
            return;
        }
        view.invalidateScroll();
    }

    /**
     * Sets the page align flag.
     *
     * @param align
     *            the new flag indicating align
     */
    @Override
    public final void setAlign(final PageAlign align) {
        invalidatePageSizes(InvalidateSizeReason.PAGE_ALIGN, null);
        invalidateScroll();
        commitZoom();
    }

    /**
     * Checks if view is initialized.
     *
     * @return true, if is initialized
     */
    protected final boolean isShown() {
        return isShown;
    }

    protected abstract boolean isPageVisibleImpl(final Page page, final ViewState viewState);

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#getFirstVisiblePage()
     */
    @Override
    public final int getFirstVisiblePage() {
        return firstVisiblePage;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#getLastVisiblePage()
     */
    @Override
    public final int getLastVisiblePage() {
        return lastVisiblePage;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#redrawView()
     */
    @Override
    public final void redrawView() {
        view.redrawView(new ViewState(this));
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#redrawView(org.ebookdroid.core.ViewState)
     */
    @Override
    public final void redrawView(final ViewState viewState) {
        view.redrawView(viewState);
    }

    @ActionMethod(ids = { R.id.actions_verticalConfigScrollUp, R.id.actions_verticalConfigScrollDown })
    public void verticalConfigScroll(final ActionEx action) {
        final Integer direction = action.getParameter("direction");
        verticalConfigScroll(direction);
    }

    protected boolean processTap(final TouchManager.Touch type, final MotionEvent e) {
        final Integer actionId = TouchManager.getAction(type, e.getX(), e.getY(), getWidth(), getHeight());
        final ActionEx action = actionId != null ? getOrCreateAction(actionId) : null;
        if (action != null) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Touch action: " + action.name + ", " + action.getMethod().toString());
            }
            action.run();
            return true;
        } else {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Touch action not found");
            }
        }
        return false;
    }

    protected class GestureListener extends SimpleOnGestureListener {

        /**
         * {@inheritDoc}
         *
         * @see android.view.GestureDetector.SimpleOnGestureListener#onDoubleTap(android.view.MotionEvent)
         */
        @Override
        public boolean onDoubleTap(final MotionEvent e) {
            // LCTX.d("onDoubleTap(" + e + ")");
            return processTap(TouchManager.Touch.DoubleTap, e);
        }

        /**
         * {@inheritDoc}
         *
         * @see android.view.GestureDetector.SimpleOnGestureListener#onDown(android.view.MotionEvent)
         */
        @Override
        public boolean onDown(final MotionEvent e) {
            view.forceFinishScroll();
            // LCTX.d("onDown(" + e + ")");
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @see android.view.GestureDetector.SimpleOnGestureListener#onFling(android.view.MotionEvent,
         *      android.view.MotionEvent, float, float)
         */
        @Override
        public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float vX, final float vY) {
            final Rect l = getScrollLimits();
            float x = vX, y = vY;
            if (Math.abs(vX / vY) < 0.5) {
                x = 0;
            }
            if (Math.abs(vY / vX) < 0.5) {
                y = 0;
            }
            // LCTX.d("onFling(" + x + ", " + y + ")");
            view.startFling(x, y, l);
            view.redrawView();
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @see android.view.GestureDetector.SimpleOnGestureListener#onScroll(android.view.MotionEvent,
         *      android.view.MotionEvent, float, float)
         */
        @Override
        public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX, final float distanceY) {
            float x = distanceX, y = distanceY;
            if (Math.abs(distanceX / distanceY) < 0.5) {
                x = 0;
            }
            if (Math.abs(distanceY / distanceX) < 0.5) {
                y = 0;
            }
            // LCTX.d("onScroll(" + x + ", " + y + ")");
            view.scrollBy((int) x, (int) y);
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @see android.view.GestureDetector.SimpleOnGestureListener#onSingleTapConfirmed(android.view.MotionEvent)
         */
        @Override
        public boolean onSingleTapConfirmed(final MotionEvent e) {
            // LCTX.d("onSingleTapConfirmed(" + e + ")");
            return processTap(TouchManager.Touch.SingleTap, e);
        }

        /**
         * {@inheritDoc}
         *
         * @see android.view.GestureDetector.SimpleOnGestureListener#onLongPress(android.view.MotionEvent)
         */
        @Override
        public void onLongPress(final MotionEvent e) {
            // LongTap operation cause side-effects
            // processTap(TouchManager.Touch.LongTap, e);
        }
    }
}
