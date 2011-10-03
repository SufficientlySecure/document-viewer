package org.ebookdroid.core;

import org.ebookdroid.core.bitmaps.BitmapManager;
import org.ebookdroid.core.bitmaps.BitmapRef;
import org.ebookdroid.core.events.ZoomListener;
import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.utils.MathUtils;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractDocumentView extends SurfaceView implements ZoomListener, IDocumentViewController,
        SurfaceHolder.Callback {

    protected static final LogContext LCTX = LogContext.ROOT.lctx("View");

    public static final int DOUBLE_TAP_TIME = 500;

    protected final IViewerActivity base;
    protected boolean isInitialized = false;
    protected final Scroller scroller;
    protected final AtomicBoolean inZoom = new AtomicBoolean();
    protected PageAlign align;

    protected DrawThread drawThread;

    protected final PageIndex pageToGo;

    protected int firstVisiblePage;
    protected int lastVisiblePage;

    protected float initialZoom;

    protected boolean layoutLocked;

    protected GestureDetector gestureDetector;

    public AbstractDocumentView(final IViewerActivity baseActivity) {
        super(baseActivity.getContext());
        this.base = baseActivity;
        this.align = SettingsManager.getBookSettings().pageAlign;
        this.firstVisiblePage = -1;
        this.lastVisiblePage = -1;
        this.scroller = new Scroller(getContext());

        this.gestureDetector = new GestureDetector(getContext(), new GestureListener());
        this.pageToGo = SettingsManager.getBookSettings().getCurrentPage();

        setKeepScreenOn(SettingsManager.getAppSettings().isKeepScreenOn());
        setFocusable(true);
        setFocusableInTouchMode(true);
        getHolder().addCallback(this);
    }

    @Override
    public final View getView() {
        return this;
    }

    @Override
    public final IViewerActivity getBase() {
        return base;
    }

    protected final Scroller getScroller() {
        return scroller;
    }

    protected final void init() {
        if (isInitialized) {
            return;
        }

        getBase().getDocumentModel().initPages(base);
        isInitialized = true;
        invalidatePageSizes(InvalidateSizeReason.INIT, null);
        invalidateScroll();

        final Page page = pageToGo.getActualPage(base.getDocumentModel(), SettingsManager.getBookSettings());
        goToPageImpl(page != null ? page.index.viewIndex : 0);
    }

    protected abstract void goToPageImpl(final int toPage);

    @Override
    protected final void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        onScrollChanged(-1, t - oldt);
    }

    protected void onScrollChanged(final int newPage, final int direction) {
        // bounds could be not updated
        if (inZoom.get()) {
            return;
        }
        redrawView();
    }

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
        base.getDecodeService().decodePage(viewState, best);

        for (final PageTreeNode node : nodesToDecode) {
            if (node != best) {
                base.getDecodeService().decodePage(viewState, node);
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
        inZoom.set(false);
        final float newZoom = base.getZoomModel().getZoom();
        SettingsManager.zoomChanged(newZoom);
        onZoomChanged(newZoom);
        initialZoom = newZoom;
    }

    protected ViewState onZoomChanged(final float newZoom) {
        if (initialZoom != newZoom) {
            BitmapManager.increateGeneration();
        }

        final ViewState viewState = calculatePageVisibility(base.getDocumentModel().getCurrentViewPageIndex(), 0,
                newZoom);

        final List<PageTreeNode> nodesToDecode = new ArrayList<PageTreeNode>();
        final List<BitmapRef> bitmapsToRecycle = new ArrayList<BitmapRef>();

        for (final Page page : getBase().getDocumentModel().getPages()) {
            page.onZoomChanged(initialZoom, viewState, nodesToDecode, bitmapsToRecycle);
        }
        BitmapManager.release(bitmapsToRecycle);

        if (!nodesToDecode.isEmpty()) {
            decodePageTreeNodes(viewState, nodesToDecode);
        }

        LCTX.d("onZoomChanged: " + viewState + " => " + nodesToDecode.size());

        return viewState;
    }

    @Override
    public final void updateMemorySettings() {
        final ViewState viewState = new ViewState(this);

        final List<PageTreeNode> nodesToDecode = new ArrayList<PageTreeNode>();
        final List<BitmapRef> bitmapsToRecycle = new ArrayList<BitmapRef>();

        for (final Page page : getBase().getDocumentModel().getPages()) {
            page.onZoomChanged(0, viewState, nodesToDecode, bitmapsToRecycle);
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
    public final void showDocument() {
        // use post to ensure that document view has width and height before decoding begin
        post(new Runnable() {

            @Override
            public void run() {
                init();
                onZoomChanged(base.getZoomModel().getZoom());
            }
        });
    }

    @Override
    public final void goToPage(final int toPage) {
        if (isInitialized) {
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
        if (!isInitialized) {
            return;
        }
        if (inZoom.compareAndSet(false, true)) {
            initialZoom = oldZoom;
        }

        stopScroller();

        final float ratio = newZoom / oldZoom;
        invalidatePageSizes(InvalidateSizeReason.ZOOM, null);

        scrollTo((int) ((getScrollX() + getWidth() / 2) * ratio - getWidth() / 2),
                (int) ((getScrollY() + getHeight() / 2) * ratio - getHeight() / 2));
        redrawView();
    }

    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        if (getBase().getMultiTouchZoom() != null) {
            if (getBase().getMultiTouchZoom().onTouchEvent(ev)) {
                return true;
            }
        }

        return gestureDetector.onTouchEvent(ev);

    }

    @Override
    public final boolean dispatchKeyEvent(final KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    verticalDpadScroll(1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    verticalDpadScroll(-1);
                    return true;

                case KeyEvent.KEYCODE_VOLUME_UP:
                    verticalConfigScroll(-1);
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    verticalConfigScroll(1);
                    return true;
            }
        }
        if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                case KeyEvent.KEYCODE_VOLUME_UP:
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    protected abstract void verticalConfigScroll(int direction);

    protected abstract void verticalDpadScroll(int direction);

    protected abstract Rect getScrollLimits();

    @Override
    public final void scrollTo(final int x, final int y) {
        final Rect l = getScrollLimits();
        super.scrollTo(MathUtils.adjust(x, l.left, l.right), MathUtils.adjust(y, l.top, l.bottom));
    }

    @Override
    public final RectF getViewRect() {
        return new RectF(getScrollX(), getScrollY(), getScrollX() + getWidth(), getScrollY() + getHeight());
    }

    @Override
    public final void computeScroll() {
        if (getScroller().computeScrollOffset()) {
            scrollTo(getScroller().getCurrX(), getScroller().getCurrY());
        }
    }

    @Override
    public void changeLayoutLock(final boolean lock) {
        layoutLocked = lock;
    }

    @Override
    protected final void onLayout(final boolean changed, final int left, final int top, final int right,
            final int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        onLayoutChanged(changed, left, top, right, bottom);
    }

    protected boolean onLayoutChanged(final boolean changed, final int left, final int top, final int right,
            final int bottom) {
        if (changed && !layoutLocked) {
            if (isInitialized) {
                ArrayList<BitmapRef> bitmapsToRecycle = new ArrayList<BitmapRef>();
                for (final Page page : base.getDocumentModel().getPages()) {
                    page.nodes.root.recycle(bitmapsToRecycle);
                }
                BitmapManager.release(bitmapsToRecycle);

                invalidatePageSizes(InvalidateSizeReason.LAYOUT, null);
                invalidateScroll();
                final float oldZoom = base.getZoomModel().getZoom();
                initialZoom = 0;
                final ViewState state = onZoomChanged(oldZoom);
                redrawView(state);
                return true;
            }
        }
        return false;
    }

    protected final void invalidateScroll() {
        if (!isInitialized) {
            return;
        }
        stopScroller();
        final float scrollScaleRatio = getScrollScaleRatio();
        scrollTo((int) (getScrollX() * scrollScaleRatio), (int) (getScrollY() * scrollScaleRatio));
    }

    private float getScrollScaleRatio() {
        final Page page = getBase().getDocumentModel().getCurrentPageObject();
        final float zoom = getBase().getZoomModel().getZoom();

        if (page == null || page.getBounds(zoom) == null) {
            return 0;
        }
        return getWidth() * zoom / page.getBounds(zoom).width();
    }

    private void stopScroller() {
        if (!getScroller().isFinished()) {
            getScroller().abortAnimation();
        }
    }

    /**
     * Sets the page align flag.
     *
     * @param align
     *            the new flag indicating align
     */
    @Override
    public final void setAlign(final PageAlign align) {
        if (align == null) {
            this.align = PageAlign.WIDTH;
        } else {
            this.align = align;
        }
        invalidatePageSizes(InvalidateSizeReason.PAGE_ALIGN, null);
        invalidateScroll();
        commitZoom();
    }

    public final PageAlign getAlign() {
        return this.align;
    }

    /**
     * Checks if view is initialized.
     *
     * @return true, if is initialized
     */
    protected final boolean isInitialized() {
        return isInitialized;
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

    public abstract void drawView(Canvas canvas, ViewState viewState);

    @Override
    public final void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
        redrawView();
    }

    @Override
    public final void redrawView() {
        redrawView(new ViewState(this));
    }

    @Override
    public final void redrawView(final ViewState viewState) {
        if (viewState != null) {
            if (drawThread != null) {
                drawThread.draw(viewState);
            }
            base.getDecodeService().updateViewState(viewState);
        }
    }

    @Override
    public final void surfaceCreated(final SurfaceHolder holder) {
        drawThread = new DrawThread(getHolder(), this);
        drawThread.start();
    }

    @Override
    public final void surfaceDestroyed(final SurfaceHolder holder) {
        drawThread.finish();
    }

    private final class GestureListener extends SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(final MotionEvent e) {
            if (SettingsManager.getAppSettings().getZoomByDoubleTap()) {
                getBase().getZoomModel().toggleZoomControls();
            }
            return true;
        }

        @Override
        public boolean onDown(final MotionEvent e) {
            if (!scroller.isFinished()) { // is flinging
                scroller.forceFinished(true); // to stop flinging on touch
            }
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
            scroller.fling(getScrollX(), getScrollY(), -(int) x, -(int) y, l.left, l.right, l.top, l.bottom);
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
            
            scrollBy((int) x, (int) y);
            return true;
        }

        @Override
        public boolean onSingleTapUp(final MotionEvent e) {
            float ts;
            if (SettingsManager.getAppSettings().getTapScroll()) {
                final int tapsize = SettingsManager.getAppSettings().getTapSize();

                ts = (float) tapsize / 100;
                if (ts > 0.5) {
                    ts = 0.5f;
                }
                if (e.getY() / getHeight() < ts) {
                    verticalConfigScroll(-1);
                } else if (e.getY() / getHeight() > (1 - ts)) {
                    verticalConfigScroll(1);
                }
                return true;
            }
            return false;
        }

    }
}
