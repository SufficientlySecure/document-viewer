package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.core.actions.AbstractComponentController;
import org.ebookdroid.core.actions.ActionEx;
import org.ebookdroid.core.actions.ActionMethod;
import org.ebookdroid.core.actions.ActionMethodDef;
import org.ebookdroid.core.actions.ActionTarget;
import org.ebookdroid.core.actions.params.Constant;
import org.ebookdroid.core.bitmaps.BitmapManager;
import org.ebookdroid.core.bitmaps.BitmapRef;
import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.touch.DefaultGestureDetector;
import org.ebookdroid.core.touch.IGestureDetector;
import org.ebookdroid.core.touch.IMultiTouchZoom;
import org.ebookdroid.core.touch.TouchManager;
import org.ebookdroid.utils.MathUtils;

import android.graphics.Canvas;
import android.graphics.Rect;
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
        @ActionMethodDef(id = R.id.actions_verticalConfigScrollDown, method = "verticalConfigScroll"),
        @ActionMethodDef(id = R.id.actions_verticalDpadScrollUp, method = "verticalDpadScroll"),
        @ActionMethodDef(id = R.id.actions_verticalDpadScrollDown, method = "verticalDpadScroll")
// no more
})
public abstract class AbstractDocumentView extends AbstractComponentController<BaseDocumentView> implements
        IDocumentViewController {

    protected static final LogContext LCTX = LogContext.ROOT.lctx("View");

    public static final int DOUBLE_TAP_TIME = 500;

    protected final IViewerActivity base;

    protected final BaseDocumentView view;

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
        createAction(R.id.actions_verticalDpadScrollUp, new Constant("direction", -1));
        createAction(R.id.actions_verticalDpadScrollDown, new Constant("direction", -1));
    }

    protected List<IGestureDetector> getGestureDetectors() {
        if (detectors == null) {
            detectors = initGestureDetectors(new ArrayList<IGestureDetector>(4));
        }
        return detectors;
    }

    protected List<IGestureDetector> initGestureDetectors(final List<IGestureDetector> list) {
        list.add(IMultiTouchZoom.Factory.createImpl(base.getZoomModel()));
        list.add(new DefaultGestureDetector(view.getContext(), new GestureListener()));
        return list;
    }

    @Override
    public final BaseDocumentView getView() {
        return view;
    }

    @Override
    public final IViewerActivity getBase() {
        return base;
    }

    @Override
    public final void init() {
        if (!isInitialized) {
            try {
                getBase().getDocumentModel().initPages(base);
            } finally {
                isInitialized = true;
            }
        }
    }

    @Override
    public final void show() {
        if (isInitialized && !isShown) {
            isShown = true;
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Showing view content...");
            }

            invalidatePageSizes(InvalidateSizeReason.INIT, null);

            final Page page = pageToGo.getActualPage(base.getDocumentModel(), SettingsManager.getBookSettings());
            final int toPage = page != null ? page.index.viewIndex : 0;

            updatePageVisibility(toPage, 0, base.getZoomModel().getZoom());
            goToPageImpl(toPage);

        } else {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("View is not initialized yet");
            }
        }
    }

    protected abstract void goToPageImpl(final int toPage);

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.IDocumentViewController#onScrollChanged(int, int)
     */
    @Override
    public void onScrollChanged(final int newPage, final int direction) {
        // bounds could be not updated
        if (inZoom.get()) {
            return;
        }
        LCTX.d("onScrollChanged(" + newPage + ", " + direction + ")");
        view.redrawView();
    }

    @Override
    public final ViewState updatePageVisibility(final int newPage, final int direction, final float zoom) {
        final ViewState viewState = calculatePageVisibility(newPage, direction, zoom);

        final List<PageTreeNode> nodesToDecode = new ArrayList<PageTreeNode>();
        final List<BitmapRef> bitmapsToRecycle = new ArrayList<BitmapRef>();

        for (final Page page : getBase().getDocumentModel().getPages()) {
            page.onPositionChanged(viewState, nodesToDecode, bitmapsToRecycle);
        }
        BitmapManager.release(bitmapsToRecycle);

        if (!nodesToDecode.isEmpty()) {
            decodePageTreeNodes(viewState, nodesToDecode);
        }

        LCTX.d("updatePageVisibility: " + viewState + " => " + nodesToDecode.size());

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
        SettingsManager.zoomChanged(newZoom);
        onZoomChanged(newZoom, true);
        initialZoom = newZoom;
    }

    protected ViewState onZoomChanged(final float newZoom, final boolean committed) {
        if (initialZoom != newZoom) {
            BitmapManager.increateGeneration();
        }

        final ViewState oldState = new ViewState(this);
        final ViewState newState = calculatePageVisibility(base.getDocumentModel().getCurrentViewPageIndex(), 0,
                newZoom);

        final List<PageTreeNode> nodesToDecode = new ArrayList<PageTreeNode>();
        final List<BitmapRef> bitmapsToRecycle = new ArrayList<BitmapRef>();

        final int minIndex = MathUtils.min(oldState.firstVisible, oldState.firstCached, newState.firstVisible,
                newState.firstCached);
        final int maxIndex = MathUtils.max(oldState.lastVisible, oldState.lastCached, newState.lastVisible,
                newState.lastCached);

        for (final Page page : getBase().getDocumentModel().getPages(minIndex, maxIndex + 1)) {
            page.onZoomChanged(initialZoom, newState, committed, nodesToDecode, bitmapsToRecycle);
        }
        BitmapManager.release(bitmapsToRecycle);

        if (!nodesToDecode.isEmpty()) {
            decodePageTreeNodes(newState, nodesToDecode);
        }

        LCTX.d("onZoomChanged: " + committed + ", " + newState + " => " + nodesToDecode.size());

        return newState;
    }

    @Override
    public final void updateMemorySettings() {
        final ViewState viewState = new ViewState(this);

        final List<PageTreeNode> nodesToDecode = new ArrayList<PageTreeNode>();
        final List<BitmapRef> bitmapsToRecycle = new ArrayList<BitmapRef>();

        for (final Page page : getBase().getDocumentModel().getPages()) {
            page.onZoomChanged(0, viewState, true, nodesToDecode, bitmapsToRecycle);
        }
        BitmapManager.release(bitmapsToRecycle);

        if (!nodesToDecode.isEmpty()) {
            decodePageTreeNodes(viewState, nodesToDecode);
        }

        LCTX.d("updateMemorySettings: " + viewState + " => " + nodesToDecode.size());
    }

    public final ViewState invalidatePages(final ViewState oldState, final Page... pages) {
        final ViewState viewState = calculatePageVisibility(pages[0].index.viewIndex, 0, oldState.zoom);

        final List<PageTreeNode> nodesToDecode = new ArrayList<PageTreeNode>();
        final List<BitmapRef> bitmapsToRecycle = new ArrayList<BitmapRef>();

        for (final Page page : pages) {
            page.onPositionChanged(viewState, nodesToDecode, bitmapsToRecycle);
        }
        BitmapManager.release(bitmapsToRecycle);

        if (!nodesToDecode.isEmpty()) {
            decodePageTreeNodes(viewState, nodesToDecode);
        }

        LCTX.d("invalidatePages: " + viewState + " => " + nodesToDecode.size());

        return viewState;
    }

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

        LCTX.d("zoomChanged(" + newZoom + ", " + oldZoom + ")");

        if (inZoom.compareAndSet(false, true)) {
            initialZoom = oldZoom;
        }

        invalidatePageSizes(InvalidateSizeReason.ZOOM, null);

        view.invalidateScroll(newZoom, oldZoom);

        view.redrawView(onZoomChanged(newZoom, false));
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

    @Override
    public boolean dispatchKeyEvent(final KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    verticalDpadScroll(1);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    verticalDpadScroll(-1);
                }
                return true;

            case KeyEvent.KEYCODE_VOLUME_UP:
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    verticalConfigScroll(-1);
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    verticalConfigScroll(1);
                }
                return true;
        }
        return false;
    }

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

    @Override
    public boolean onLayoutChanged(final boolean layoutChanged, final boolean layoutLocked, final Rect oldLaout,
            final Rect newLayout) {
        LCTX.d("onLayoutChanged(" + layoutChanged + ", " + layoutLocked + "," + oldLaout + ", " + newLayout + ")");
        if (layoutChanged && !layoutLocked) {
            if (isShown) {
                final ArrayList<BitmapRef> bitmapsToRecycle = new ArrayList<BitmapRef>();
                for (final Page page : base.getDocumentModel().getPages()) {
                    page.nodes.root.recycle(bitmapsToRecycle);
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

    @Override
    public final int getFirstVisiblePage() {
        return firstVisiblePage;
    }

    @Override
    public final int getLastVisiblePage() {
        return lastVisiblePage;
    }

    @Override
    public abstract void drawView(Canvas canvas, ViewState viewState);

    @Override
    public final void redrawView() {
        view.redrawView(new ViewState(this));
    }

    @Override
    public final void redrawView(final ViewState viewState) {
        view.redrawView(viewState);
    }

    @ActionMethod(ids = { R.id.actions_verticalConfigScrollUp, R.id.actions_verticalConfigScrollDown })
    public void verticalConfigScroll(final ActionEx action) {
        final Integer direction = action.getParameter("direction");
        verticalConfigScroll(direction);
    }

    @ActionMethod(ids = { R.id.actions_verticalDpadScrollUp, R.id.actions_verticalDpadScrollDown })
    public void verticalDpadScroll(final ActionEx action) {
        final Integer direction = action.getParameter("direction");
        verticalDpadScroll(direction);
    }

    protected boolean processTap(final TouchManager.Touch type, final MotionEvent e) {
        final Integer actionId = TouchManager.getAction(type, e.getX(), e.getY(), getWidth(), getHeight());
        final ActionEx action = actionId != null ? getOrCreateAction(actionId) : null;
        if (action != null) {
            LCTX.d("Touch action: " + action.name + ", " + action.getMethod().toString());
            action.run();
            return true;
        } else {
            LCTX.d("Touch action not found");
        }
        return false;
    }

    protected class GestureListener extends SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(final MotionEvent e) {
            // LCTX.d("onDoubleTap(" + e + ")");
            return processTap(TouchManager.Touch.DoubleTap, e);
        }

        @Override
        public boolean onDown(final MotionEvent e) {
            view.forceFinishScroll();
            // LCTX.d("onDown(" + e + ")");
            return true;
        }

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

        @Override
        public boolean onSingleTapConfirmed(final MotionEvent e) {
            // LCTX.d("onSingleTapConfirmed(" + e + ")");
            return processTap(TouchManager.Touch.SingleTap, e);
        }

        @Override
        public void onLongPress(final MotionEvent e) {
            // TODO Auto-generated method stub
            processTap(TouchManager.Touch.LongTap, e);
        }
    }
}
