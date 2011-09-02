package org.ebookdroid.core;

import org.ebookdroid.core.events.ZoomListener;
import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.utils.MathUtils;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.VelocityTracker;
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
    protected float lastX;
    protected float lastY;
    protected VelocityTracker velocityTracker;
    protected final Scroller scroller;
    protected final AtomicBoolean inZoom = new AtomicBoolean();
    protected long lastDownEventTime;
    protected PageAlign align;
    protected Boolean touchInTapZone = null;

    protected DrawThread drawThread;

    protected final PageIndex pageToGo;

    protected int firstVisiblePage;
    protected int lastVisiblePage;

    protected float initialZoom;

    public AbstractDocumentView(final IViewerActivity baseActivity) {
        super(baseActivity.getContext());
        this.base = baseActivity;
        this.align = SettingsManager.getBookSettings().getPageAlign();
        this.firstVisiblePage = -1;
        this.lastVisiblePage = -1;
        this.scroller = new Scroller(getContext());
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
        // on scrollChanged can be called from scrollTo just after new layout applied so we should wait for relayout
        base.getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                updatePageVisibility(newPage, direction, getBase().getZoomModel().getZoom());
            }
        });
    }

    public final ViewState updatePageVisibility(final int newPage, final int direction, final float zoom) {
        final ViewState viewState = calculatePageVisibility(newPage, direction, zoom);

        final List<PageTreeNode> nodesToDecode = new ArrayList<PageTreeNode>();

        for (final Page page : getBase().getDocumentModel().getPages()) {
            page.onPositionChanged(viewState, nodesToDecode);
        }

        if (!nodesToDecode.isEmpty()) {
            decodePageTreeNodes(viewState, nodesToDecode);
        }

        LCTX.d("updatePageVisibility: " + viewState + " => " + nodesToDecode.size());

        return viewState;
    }

    protected final void decodePageTreeNodes(final ViewState viewState, final List<PageTreeNode> nodesToDecode) {
        PageTreeNode best = Collections.min(nodesToDecode, new PageTreeNodeComparator(viewState));
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

    @Override
    public final void commitZoom() {
        if (inZoom.compareAndSet(true, false)) {
            final float newZoom = base.getZoomModel().getZoom();
            SettingsManager.zoomChanged(newZoom);
            initialZoom = newZoom;
            onZoomChanged(newZoom);
        }
    }

    protected final void onZoomChanged(final float newZoom) {
        final ViewState viewState = calculatePageVisibility(base.getDocumentModel().getCurrentViewPageIndex(), 0,
                newZoom);

        final List<PageTreeNode> nodesToDecode = new ArrayList<PageTreeNode>();

        for (final Page page : getBase().getDocumentModel().getPages()) {
            page.onZoomChanged(initialZoom, viewState, nodesToDecode);
        }
        if (!nodesToDecode.isEmpty()) {
            decodePageTreeNodes(viewState, nodesToDecode);
        }

        LCTX.d("onZoomChanged: " + viewState + " => " + nodesToDecode.size());
    }

    @Override
    public final void updateMemorySettings() {
        final ViewState viewState = new ViewState(this);

        final List<PageTreeNode> nodesToDecode = new ArrayList<PageTreeNode>();

        for (final Page page : getBase().getDocumentModel().getPages()) {
            page.onZoomChanged(0, viewState, nodesToDecode);
        }
        if (!nodesToDecode.isEmpty()) {
            decodePageTreeNodes(viewState, nodesToDecode);
        }

        LCTX.d("updateMemorySettings: " + viewState + " => " + nodesToDecode.size());
    }

    public final ViewState invalidatePages(ViewState oldState, final Page... pages) {
        final ViewState viewState = calculatePageVisibility(pages[0].index.viewIndex, 0, oldState.zoom);

        final List<PageTreeNode> nodesToDecode = new ArrayList<PageTreeNode>();
        for (final Page page : pages) {
            page.onPositionChanged(viewState, nodesToDecode);
        }
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

    @Override
    public final void zoomChanged(final float newZoom, final float oldZoom) {
        if (!isInitialized) {
            return;
        }
        // if (LCTX.isDebugEnabled()) {
        // LCTX.d("Zoom changed: " + oldZoom + " -> " + newZoom);
        // }
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
        super.onTouchEvent(ev);

        if (getBase().getMultiTouchZoom() != null) {
            if (getBase().getMultiTouchZoom().onTouchEvent(ev)) {
                return true;
            }

            if (getBase().getMultiTouchZoom().isResetLastPointAfterZoom()) {
                setLastPosition(ev);
                getBase().getMultiTouchZoom().setResetLastPointAfterZoom(false);
            }
        }

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(ev);

        boolean inTap = false;
        float ts = 0;
        if (SettingsManager.getAppSettings().getTapScroll()) {
            final int tapsize = SettingsManager.getAppSettings().getTapSize();

            ts = (float) tapsize / 100;
            if (ts > 0.5) {
                ts = 0.5f;
            }
            if ((ev.getY() / getHeight() < ts) || (ev.getY() / getHeight() > (1 - ts))) {
                inTap = true;
            }
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                stopScroller();
                setLastPosition(ev);
                if (ev.getEventTime() - lastDownEventTime < DOUBLE_TAP_TIME) {
                    if (SettingsManager.getAppSettings().getZoomByDoubleTap()) {
                        getBase().getZoomModel().toggleZoomControls();
                    }
                } else {
                    lastDownEventTime = ev.getEventTime();
                }
                touchInTapZone = inTap;

                break;
            case MotionEvent.ACTION_MOVE:
                if (getSquareDistanceToLast(ev) >= 100) {
                    lastDownEventTime = 0;
                }
                scrollBy((int) (lastX - ev.getX()), (int) (lastY - ev.getY()));
                setLastPosition(ev);
                redrawView();
                break;
            case MotionEvent.ACTION_UP:
                velocityTracker.computeCurrentVelocity(1000);
                final Rect l = getScrollLimits();
                getScroller().fling(getScrollX(), getScrollY(), (int) -velocityTracker.getXVelocity(),
                        (int) -velocityTracker.getYVelocity(), l.left, l.right, l.top, l.bottom);
                velocityTracker.recycle();
                velocityTracker = null;
                if (getSquareDistanceToLast(ev) >= 100) {
                    lastDownEventTime = 0;
                }

                if (inTap && (touchInTapZone == null || touchInTapZone.booleanValue())) {
                    if (ev.getY() / getHeight() < ts) {
                        verticalConfigScroll(-1);
                    } else if (ev.getY() / getHeight() > (1 - ts)) {
                        verticalConfigScroll(1);
                    }
                }

                touchInTapZone = null;
                break;
        }
        return true;
    }

    public final long getLastDownEventTime() {
        return lastDownEventTime;
    }

    public final void setLastDownEventTime(long lastDownEventTime) {
        this.lastDownEventTime = lastDownEventTime;
    }

    public final void setLastPosition(final MotionEvent ev) {
        lastX = ev.getX();
        lastY = ev.getY();
    }

    public final float getSquareDistanceToLast(final MotionEvent ev) {
        return (ev.getX() - lastX) * (ev.getX() - lastX) + (ev.getY() - lastY) * (ev.getY() - lastY);
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
    protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        invalidatePageSizes(InvalidateSizeReason.LAYOUT, null);
        invalidateScroll();
        commitZoom();
        redrawView();
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
}
