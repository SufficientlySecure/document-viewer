package org.ebookdroid.core;

import org.ebookdroid.core.events.ZoomListener;

import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.Scroller;

public abstract class AbstractDocumentView extends View implements ZoomListener, IDocumentViewController {

    private final IViewerActivity base;
    private boolean isInitialized = false;
    private int pageToGoTo;
    private float lastX;
    private float lastY;
    protected VelocityTracker velocityTracker;
    private final Scroller scroller;
    private RectF viewRect;
    private boolean inZoom;
    private long lastDownEventTime;
    private static final int DOUBLE_TAP_TIME = 500;
    private PageAlign align;

    public AbstractDocumentView(final IViewerActivity baseActivity) {
        super(baseActivity.getContext());
        this.base = baseActivity;
        this.align = base.getBookSettings().getPageAlign();
        setKeepScreenOn(true);
        scroller = new Scroller(getContext());
        setFocusable(true);
        setFocusableInTouchMode(true);
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
        invalidatePageSizes();
        invalidateScroll();
        goToPageImpl(pageToGoTo);
    }

    protected abstract void goToPageImpl(final int toPage);

    @Override
    protected void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        onScrollChanged();
    }

    protected void onScrollChanged() {
        // bounds could be not updated
        if (inZoom) {
            return;
        }
        // on scrollChanged can be called from scrollTo just after new layout applied so we should wait for relayout
        post(new Runnable() {

            @Override
            public void run() {
                updatePageVisibility();
            }
        });
    }

    @Override
    public void updatePageVisibility() {
        for (final Page page : getBase().getDocumentModel().getPages().values()) {
            page.updateVisibility();
        }
    }

    @Override
    public void commitZoom() {
        for (final Page page : getBase().getDocumentModel().getPages().values()) {
            page.invalidate();
        }
        inZoom = false;
    }

    @Override
    public void showDocument() {
        // use post to ensure that document view has width and height before decoding begin
        post(new Runnable() {

            @Override
            public void run() {
                init();
                updatePageVisibility();
            }
        });
    }

    @Override
    public void goToPage(final int toPage) {
        if (isInitialized) {
            goToPageImpl(toPage);
        } else {
            pageToGoTo = toPage;
        }
    }

    public abstract int getCurrentPage();

    @Override
    public void zoomChanged(final float newZoom, final float oldZoom) {
        inZoom = true;
        stopScroller();
        final float ratio = newZoom / oldZoom;
        invalidatePageSizes();
        scrollTo((int) ((getScrollX() + getWidth() / 2) * ratio - getWidth() / 2),
                (int) ((getScrollY() + getHeight() / 2) * ratio - getHeight() / 2));
        postInvalidate();
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

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                stopScroller();
                setLastPosition(ev);
                if (ev.getEventTime() - lastDownEventTime < DOUBLE_TAP_TIME) {
                    // zoomModel.toggleZoomControls();
                } else {
                    lastDownEventTime = ev.getEventTime();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                scrollBy((int) (lastX - ev.getX()), (int) (lastY - ev.getY()));
                setLastPosition(ev);
                break;
            case MotionEvent.ACTION_UP:
                velocityTracker.computeCurrentVelocity(1000);
                getScroller().fling(getScrollX(), getScrollY(), (int) -velocityTracker.getXVelocity(),
                        (int) -velocityTracker.getYVelocity(), getLeftLimit(), getRightLimit(), getTopLimit(),
                        getBottomLimit());
                velocityTracker.recycle();
                velocityTracker = null;
                if (base.getAppSettings().getTapScroll()) {
                    final int tapsize = base.getAppSettings().getTapSize();

                    float ts = (float) tapsize / 100;
                    if (ts > 0.5) {
                        ts = 0.5f;
                    }

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
    public boolean dispatchKeyEvent(final KeyEvent event) {
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
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                case KeyEvent.KEYCODE_VOLUME_UP:
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    protected abstract void verticalConfigScroll(int direction);

    protected abstract void verticalDpadScroll(int direction);

    protected abstract int getTopLimit();

    protected abstract int getLeftLimit();

    protected abstract int getBottomLimit();

    protected abstract int getRightLimit();

    @Override
    public void scrollTo(final int x, final int y) {
        super.scrollTo(Math.min(Math.max(x, getLeftLimit()), getRightLimit()),
                Math.min(Math.max(y, getTopLimit()), getBottomLimit()));
        viewRect = null;
    }

    protected RectF getViewRect() {
        if (viewRect == null) {
            viewRect = new RectF(getScrollX(), getScrollY(), getScrollX() + getWidth(), getScrollY() + getHeight());
        }
        return viewRect;
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
        invalidatePageSizes();
        invalidateScroll();
        commitZoom();
    }

    /**
     * Invalidate page sizes.
     */
    @Override
    public abstract void invalidatePageSizes();

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
        invalidatePageSizes();
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

    /**
     * Returns if given page tree node should be kept in memory.
     *
     * @param pageTreeNode the page tree node
     * @return true, if successful
     * @see org.ebookdroid.core.IDocumentViewController#shouldKeptInMemory(org.ebookdroid.core.PageTreeNode)
     */
    public final boolean shouldKeptInMemory(final PageTreeNode pageTreeNode) {
        return (pageTreeNode.getPageIndex() >= getBase().getDocumentModel().getFirstVisiblePage()
                - getBase().getAppSettings().getPagesInMemory())
                && (pageTreeNode.getPageIndex() <= getBase().getDocumentModel().getLastVisiblePage()
                        + getBase().getAppSettings().getPagesInMemory());
    }

    @Override
    public abstract boolean isPageVisible(Page page);

}
