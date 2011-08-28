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

    private final IViewerActivity base;
    private boolean isInitialized = false;
    private float lastX;
    private float lastY;
    protected VelocityTracker velocityTracker;
    private final Scroller scroller;
    private final AtomicBoolean inZoom = new AtomicBoolean();
    private long lastDownEventTime;
    private static final int DOUBLE_TAP_TIME = 500;
    private PageAlign align;
    private boolean touchInTapZone = false;

    private DrawThread drawThread;

    final PageIndex pageToGo;

    int firstVisiblePage;
    int lastVisiblePage;

    float initialZoom;

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
    public View getView() {
        return this;
    }

    @Override
    public IViewerActivity getBase() {
        return base;
    }

    protected Scroller getScroller() {
        return scroller;
    }

    protected void init() {
        if (isInitialized) {
            return;
        }

        getBase().getDocumentModel().initPages(base);
        isInitialized = true;
        invalidatePageSizes(InvalidateSizeReason.INIT, null);
        invalidateScroll();

        Page page = pageToGo.getActualPage(base.getDocumentModel(), SettingsManager.getBookSettings());
        goToPageImpl(page != null ? page.index.viewIndex : 0);
    }

    protected abstract void goToPageImpl(final int toPage);

    @Override
    protected void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
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
                updatePageVisibility(newPage, direction);
            }
        });
    }

    @Override
    public void updatePageVisibility(final int newPage, final int direction) {
        calculatePageVisibility(newPage, direction);

        final RectF viewRect = getViewRect();
        final List<PageTreeNode> nodesToDecode = new ArrayList<PageTreeNode>();

        for (final Page page : getBase().getDocumentModel().getPages()) {
            page.onPositionChanged(viewRect, nodesToDecode);
        }
        if (!nodesToDecode.isEmpty()) {
            decodePageTreeNodes(nodesToDecode);
        }
    }

    protected void decodePageTreeNodes(final List<PageTreeNode> nodesToDecode) {
        Collections.sort(nodesToDecode, this);
        for (final PageTreeNode pageTreeNode : nodesToDecode) {
            final int width = base.getView().getWidth();
            final float zoom = base.getZoomModel().getZoom() * pageTreeNode.page.getTargetRectScale();
            base.getDecodeService().decodePage(pageTreeNode, width, zoom, pageTreeNode);
        }
    }

    protected void calculatePageVisibility(final int newPage, final int direction) {
        final Page[] pages = getBase().getDocumentModel().getPages();

        if (newPage != -1) {
            firstVisiblePage = newPage;
            lastVisiblePage = newPage;
            while (firstVisiblePage > 0) {
                final int index = firstVisiblePage - 1;
                if (!isPageVisibleImpl(pages[index])) {
                    break;
                }
                firstVisiblePage = index;
            }
            while (lastVisiblePage < pages.length - 1) {
                final int index = lastVisiblePage + 1;
                if (!isPageVisibleImpl(pages[index])) {
                    break;
                }
                lastVisiblePage = index;
            }
        } else if (direction < 0 && lastVisiblePage != -1) {
            for (int i = lastVisiblePage; i >= 0; i--) {
                if (!isPageVisibleImpl(pages[i])) {
                    continue;
                } else {
                    lastVisiblePage = i;
                    break;
                }
            }
            firstVisiblePage = lastVisiblePage;
            while (firstVisiblePage > 0) {
                final int index = firstVisiblePage - 1;
                if (!isPageVisibleImpl(pages[index])) {
                    break;
                }
                firstVisiblePage = index;
            }

        } else if (direction > 0 && firstVisiblePage != -1) {
            for (int i = firstVisiblePage; i < pages.length; i++) {
                if (!isPageVisibleImpl(pages[i])) {
                    continue;
                } else {
                    firstVisiblePage = i;
                    break;
                }
            }
            lastVisiblePage = firstVisiblePage;
            while (lastVisiblePage < pages.length - 1) {
                final int index = lastVisiblePage + 1;
                if (!isPageVisibleImpl(pages[index])) {
                    break;
                }
                lastVisiblePage = index;
            }
        } else {
            firstVisiblePage = -1;
            lastVisiblePage = 1;
            for (final Page page : getBase().getDocumentModel().getPages()) {
                if (isPageVisibleImpl(page)) {
                    if (firstVisiblePage == -1) {
                        firstVisiblePage = page.index.viewIndex;
                    }
                    lastVisiblePage = page.index.viewIndex;
                } else if (firstVisiblePage != -1) {
                    break;
                }
            }
        }
        // if (LCTX.isDebugEnabled()) {
        // LCTX.d("Visible pages: " + firstVisiblePage + " " + lastVisiblePage);
        // }
    }

    @Override
    public void commitZoom() {
        if (inZoom.compareAndSet(true, false)) {
            final float newZoom = base.getZoomModel().getZoom();
            SettingsManager.zoomChanged(newZoom);
            onZoomChanged(newZoom);
        }
    }

    private void onZoomChanged(final float newZoom) {
        calculatePageVisibility(base.getDocumentModel().getCurrentViewPageIndex(), 0);

        final RectF viewRect = getViewRect();
        final List<PageTreeNode> nodesToDecode = new ArrayList<PageTreeNode>();

        for (final Page page : getBase().getDocumentModel().getPages()) {
            page.onZoomChanged(initialZoom, newZoom, viewRect, nodesToDecode);
        }
        if (!nodesToDecode.isEmpty()) {
            decodePageTreeNodes(nodesToDecode);
        }
    }

    public void invalidatePages(final Page... pages) {
        final RectF viewRect = getViewRect();
        final List<PageTreeNode> nodesToDecode = new ArrayList<PageTreeNode>();
        for (final Page page : pages) {
            page.onPositionChanged(viewRect, nodesToDecode);
        }
        if (!nodesToDecode.isEmpty()) {
            decodePageTreeNodes(nodesToDecode);
        }
    }

    @Override
    public void showDocument() {
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
    public void goToPage(final int toPage) {
        if (isInitialized) {
            goToPageImpl(toPage);
        }
    }

    public abstract int getCurrentPage();

    @Override
    public void zoomChanged(final float newZoom, final float oldZoom) {
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
                    // zoomModel.toggleZoomControls();
                } else {
                    lastDownEventTime = ev.getEventTime();
                }
                touchInTapZone = inTap;

                break;
            case MotionEvent.ACTION_MOVE:
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

                if (inTap && touchInTapZone) {
                    if (ev.getY() / getHeight() < ts) {
                        verticalConfigScroll(-1);
                    } else if (ev.getY() / getHeight() > (1 - ts)) {
                        verticalConfigScroll(1);
                    }
                }
                break;
        }
        return true;
    }

    protected void setLastPosition(final MotionEvent ev) {
        lastX = ev.getX();
        lastY = ev.getY();
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
    public RectF getViewRect() {
        return new RectF(getScrollX(), getScrollY(), getScrollX() + getWidth(), getScrollY() + getHeight());
    }

    @Override
    public void computeScroll() {
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

    protected void invalidateScroll() {
        if (!isInitialized) {
            return;
        }
        stopScroller();
        final float scrollScaleRatio = getScrollScaleRatio();
        scrollTo((int) (getScrollX() * scrollScaleRatio), (int) (getScrollY() * scrollScaleRatio));
    }

    private float getScrollScaleRatio() {
        final Page page = getBase().getDocumentModel().getCurrentPageObject();
        if (page == null || page.getBounds() == null) {
            return 0;
        }
        final float v = getBase().getZoomModel().getZoom();
        return getWidth() * v / page.getBounds().width();
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
    public void setAlign(final PageAlign align) {
        if (align == null) {
            this.align = PageAlign.WIDTH;
        } else {
            this.align = align;
        }
        invalidatePageSizes(InvalidateSizeReason.PAGE_ALIGN, null);
        invalidateScroll();
        commitZoom();
    }

    public PageAlign getAlign() {
        return this.align;
    }

    /**
     * Checks if view is initialized.
     *
     * @return true, if is initialized
     */
    protected boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public final boolean isPageVisible(final Page page) {
        return firstVisiblePage <= page.index.viewIndex && page.index.viewIndex <= lastVisiblePage;
    }

    protected abstract boolean isPageVisibleImpl(final Page page);

    @Override
    public int getFirstVisiblePage() {
        return firstVisiblePage;
    }

    @Override
    public int getLastVisiblePage() {
        return lastVisiblePage;
    }

    public abstract void drawView(Canvas canvas, RectF viewRect2);

    @Override
    public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
        redrawView();
    }

    @Override
    public void redrawView() {
        if (drawThread != null) {
            drawThread.draw(getViewRect());
        }
    }

    @Override
    public void surfaceCreated(final SurfaceHolder holder) {
        drawThread = new DrawThread(getHolder(), this);
        drawThread.start();
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder) {
        drawThread.finish();
    }
}
