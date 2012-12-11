package org.ebookdroid.ui.viewer.viewers;

import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.EventPool;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.IView;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.emdev.ui.gl.GLCanvas;
import org.emdev.ui.gl.GLRootView;
import org.emdev.ui.widget.Flinger;
import org.emdev.utils.concurrent.Flag;

public final class GLView extends GLRootView implements IView, SurfaceHolder.Callback {

    protected final IActivityController base;

    protected final Flinger scroller;

    protected DrawQueue drawQueue;

    protected ScrollEventThread scrollThread;

    protected boolean layoutLocked;

    protected final AtomicReference<Rect> layout = new AtomicReference<Rect>();

    protected final Flag layoutFlag = new Flag();

    protected final FullScreenCallback fullScreenCallback;

    public GLView(final IActivityController baseActivity) {
        super(baseActivity.getContext());

        this.base = baseActivity;
        this.scroller = new Flinger();

        setKeepScreenOn(AppSettings.current().keepScreenOn);
        setFocusable(true);
        setFocusableInTouchMode(true);

        fullScreenCallback = FullScreenCallback.get(baseActivity.getActivity(), this);

        drawQueue = new DrawQueue();
        scrollThread = new ScrollEventThread(base, this);
        scrollThread.start();
    }

    protected void draw(GLCanvas canvas) {
        ViewState viewState = drawQueue.takeLastTask();
        if (viewState == null) {
            viewState = ViewState.get(base.getDocumentController());
            viewState.addedToDrawQueue();
        }
        EventPool.newGLEventDraw(viewState, canvas).process().releaseAfterDraw();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#getView()
     */
    @Override
    public final View getView() {
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#getBase()
     */
    @Override
    public final IActivityController getBase() {
        return base;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#invalidateScroll()
     */
    @Override
    public final void invalidateScroll() {
        stopScroller();

        final float scrollScaleRatio = getScrollScaleRatio();
        scrollTo((int) (getScrollX() * scrollScaleRatio), (int) (getScrollY() * scrollScaleRatio));
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#invalidateScroll(float, float)
     */
    @Override
    public final void invalidateScroll(final float newZoom, final float oldZoom) {
        stopScroller();

        final float ratio = newZoom / oldZoom;
        final float halfWidth = getWidth() / 2.0f;
        final float halfHeight = getHeight() / 2.0f;

        final int x = (int) ((getScrollX() + halfWidth) * ratio - halfWidth);
        final int y = (int) ((getScrollY() + halfHeight) * ratio - halfHeight);

        // if (LCTX.isDebugEnabled()) {
        // LCTX.d("invalidateScroll(" + newZoom + ", " + oldZoom + "): " + x + ", " + y);
        // }
        scrollTo(x, y);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#startPageScroll(int, int)
     */
    @Override
    public void startPageScroll(final int dx, final int dy) {
        scroller.startScroll(getScrollX(), getScrollY(), dx, dy);
        redrawView();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#startFling(float, float, android.graphics.Rect)
     */
    @Override
    public void startFling(final float vX, final float vY, final Rect limits) {
        scroller.fling(getScrollX(), getScrollY(), -(int) vX, -(int) vY, limits.left, limits.right, limits.top,
                limits.bottom);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#continueScroll()
     */
    @Override
    public void continueScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#forceFinishScroll()
     */
    @Override
    public void forceFinishScroll() {
        scroller.forceFinished(); // to stop flinging on touch
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#isScrollFinished()
     */
    @Override
    public boolean isScrollFinished() {
        return scroller.isFinished();
    }

    /**
     * {@inheritDoc}
     *
     * @see android.view.View#onScrollChanged(int, int, int, int)
     */
    @Override
    public void onScrollChanged(final int curX, final int curY, final int oldX, final int oldY) {
        super.onScrollChanged(curX, curY, oldX, oldY);
        scrollThread.onScrollChanged(curX, curY, oldX, oldY);
    }

    /**
     * {@inheritDoc}
     *
     * @see android.view.View#onTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        mRenderLock.lock();
        try {
            checkFullScreenMode();

            if (base.getDocumentController().onTouchEvent(ev)) {
                return true;
            }
            return super.onTouchEvent(ev);
        } finally {
            mRenderLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#checkFullScreenMode()
     */
    @Override
    public void checkFullScreenMode() {
        if (fullScreenCallback != null) {
            fullScreenCallback.checkFullScreenMode();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#scrollTo(int, int)
     */
    @Override
    public final void scrollTo(final int x, final int y) {
        scrollThread.scrollTo(x, y);
    }

    @Override
    public void _scrollTo(int x, int y) {
        super.scrollTo(x, y);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#getViewRect()
     */
    @Override
    public final RectF getViewRect() {
        return new RectF(getScrollX(), getScrollY(), getScrollX() + getWidth(), getScrollY() + getHeight());
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#changeLayoutLock(boolean)
     */
    @Override
    public void changeLayoutLock(final boolean lock) {
        post(new Runnable() {

            @Override
            public void run() {
                layoutLocked = lock;
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#isLayoutLocked()
     */
    @Override
    public boolean isLayoutLocked() {
        return layoutLocked;
    }

    /**
     * {@inheritDoc}
     *
     * @see android.view.View#onLayout(boolean, int, int, int, int)
     */
    @Override
    protected final void onLayout(final boolean layoutChanged, final int left, final int top, final int right,
            final int bottom) {
        super.onLayout(layoutChanged, left, top, right, bottom);

        final Rect oldLayout = layout.getAndSet(new Rect(left, top, right, bottom));
        base.getDocumentController().onLayoutChanged(layoutChanged, layoutLocked, oldLayout, layout.get());

        if (oldLayout == null) {
            layoutFlag.set();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#waitForInitialization()
     */
    @Override
    public final void waitForInitialization() {
        while (!layoutFlag.get()) {
            layoutFlag.waitFor(TimeUnit.SECONDS, 1);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#onDestroy()
     */
    @Override
    public void onDestroy() {
        layoutFlag.set();
        scrollThread.finish();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#getScrollScaleRatio()
     */
    @Override
    public float getScrollScaleRatio() {
        final Page page = base.getDocumentModel().getCurrentPageObject();
        if (page == null) {
            return 0;
        }

        final float zoom = base.getZoomModel().getZoom();
        return getWidth() * zoom / page.getBounds(zoom).width();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#stopScroller()
     */
    @Override
    public void stopScroller() {
        if (!scroller.isFinished()) {
            scroller.abortAnimation();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#redrawView()
     */
    @Override
    public final void redrawView() {
        redrawView(ViewState.get(base.getDocumentController()));
    }

    @Override
    public final void redrawView(final ViewState viewState) {
        if (viewState != null) {
            if (drawQueue != null) {
                drawQueue.draw(viewState);
            }
            final DecodeService ds = base.getDecodeService();
            if (ds != null) {
                ds.updateViewState(viewState);
            }
            requestRender();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#getBase(android.graphics.RectF)
     */
    @Override
    public PointF getBase(final RectF viewRect) {
        return new PointF(viewRect.left, viewRect.top);
    }
}
