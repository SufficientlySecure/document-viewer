package org.ebookdroid.ui.viewer.viewers;

import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.EventPool;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.IView;
import org.ebookdroid.ui.viewer.IViewController;

import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.emdev.utils.MathUtils;
import org.emdev.utils.concurrent.Flag;

public final class BaseView extends View implements IView {

    private static final PointF BASE_POINT = new PointF(0, 0);

    protected final IActivityController base;

    protected final Scroller scroller;

    protected PageAlign align;

    protected DrawThread drawThread;

    protected boolean layoutLocked;

    protected final AtomicReference<Rect> layout = new AtomicReference<Rect>();

    protected final Flag layoutFlag = new Flag();

    public BaseView(final IActivityController baseActivity) {
        super(baseActivity.getContext());
        this.base = baseActivity;
        this.scroller = new Scroller(getContext());

        setKeepScreenOn(AppSettings.current().keepScreenOn);
        setFocusable(true);
        setFocusableInTouchMode(true);

        drawThread = new DrawThread(null);
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
     * @see org.ebookdroid.ui.viewer.IView#getScroller()
     */
    @Override
    public final Scroller getScroller() {
        return scroller;
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
        scrollTo((int) ((getScrollX() + getWidth() / 2) * ratio - getWidth() / 2),
                (int) ((getScrollY() + getHeight() / 2) * ratio - getHeight() / 2));
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
        if (!scroller.isFinished()) { // is flinging
            scroller.forceFinished(true); // to stop flinging on touch
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see android.view.View#onScrollChanged(int, int, int, int)
     */
    @Override
    protected final void onScrollChanged(final int curX, final int curY, final int oldX, final int oldY) {
        super.onScrollChanged(curX, curY, oldX, oldY);

        base.getDocumentController().onScrollChanged(curX - oldX, curY - oldY);
    }

    /**
     * {@inheritDoc}
     * 
     * @see android.view.View#onTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        if (base.getDocumentController().onTouchEvent(ev)) {
            return true;
        }
        return super.onTouchEvent(ev);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IView#scrollTo(int, int)
     */
    @Override
    public final void scrollTo(final int x, final int y) {
        final Runnable r = new Runnable() {

            @Override
            public void run() {
                final IViewController dc = base.getDocumentController();
                final DocumentModel dm = base.getDocumentModel();
                if (dc != null && dm != null) {
                    final Rect l = dc.getScrollLimits();
                    BaseView.super.scrollTo(MathUtils.adjust(x, l.left, l.right), MathUtils.adjust(y, l.top, l.bottom));
                }
            }
        };

        base.getActivity().runOnUiThread(r);
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
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IView#getScrollScaleRatio()
     */
    @Override
    public float getScrollScaleRatio() {
        final Page page = getBase().getDocumentModel().getCurrentPageObject();
        final float zoom = getBase().getZoomModel().getZoom();

        if (page == null || page.getBounds(zoom) == null) {
            return 0;
        }
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
        redrawView(new ViewState(base.getDocumentController()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IView#redrawView(org.ebookdroid.core.ViewState)
     */
    @Override
    public final void redrawView(final ViewState viewState) {
        if (viewState != null) {
            if (drawThread != null) {
                drawThread.draw(viewState);
            }
            final DecodeService ds = base.getDecodeService();
            if (ds != null) {
                ds.updateViewState(viewState);
            }
            postInvalidate();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(final Canvas canvas) {
        ViewState viewState = drawThread.takeTask(1, TimeUnit.MILLISECONDS, true);
        if (viewState == null) {
            viewState = new ViewState(base.getDocumentController());
        }
        EventPool.newEventDraw(viewState, canvas).process();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IView#getBase(android.graphics.RectF)
     */
    @Override
    public PointF getBase(final RectF viewRect) {
        return BASE_POINT;
    }
}
