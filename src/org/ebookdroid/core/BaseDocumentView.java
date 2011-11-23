package org.ebookdroid.core;

import org.ebookdroid.core.DrawThread.DrawTask;
import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.utils.Flag;
import org.ebookdroid.utils.MathUtils;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class BaseDocumentView extends View {

    protected static final LogContext LCTX = LogContext.ROOT.lctx("View");

    protected final IViewerActivity base;

    protected final Scroller scroller;

    protected PageAlign align;

    protected DrawThread drawThread;

    protected boolean layoutLocked;

    public BaseDocumentView(final IViewerActivity baseActivity) {
        super(baseActivity.getContext());
        this.base = baseActivity;
        this.scroller = new Scroller(getContext());

        setKeepScreenOn(SettingsManager.getAppSettings().isKeepScreenOn());
        setFocusable(true);
        setFocusableInTouchMode(true);
        // getHolder().addCallback(this);
        drawThread = new DrawThread(null);
    }

    public final BaseDocumentView getView() {
        return this;
    }

    public final IViewerActivity getBase() {
        return base;
    }

    public final Scroller getScroller() {
        return scroller;
    }

    public final void invalidateScroll() {
        stopScroller();

        final float scrollScaleRatio = getScrollScaleRatio();
        scrollTo((int) (getScrollX() * scrollScaleRatio), (int) (getScrollY() * scrollScaleRatio));
    }

    public final void invalidateScroll(final float newZoom, final float oldZoom) {
        stopScroller();

        final float ratio = newZoom / oldZoom;
        scrollTo((int) ((getScrollX() + getWidth() / 2) * ratio - getWidth() / 2),
                (int) ((getScrollY() + getHeight() / 2) * ratio - getHeight() / 2));
    }

    public void startPageScroll(final int dy) {
        scroller.startScroll(getScrollX(), getScrollY(), 0, dy);
        redrawView();
    }

    public void startFling(float vX, float vY, final Rect limits) {
        scroller.fling(getScrollX(), getScrollY(), -(int) vX, -(int) vY, limits.left, limits.right, limits.top,
                limits.bottom);
    }

    public void continueScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
        }
    }

    public void forceFinishScroll() {
        if (!scroller.isFinished()) { // is flinging
            scroller.forceFinished(true); // to stop flinging on touch
        }
    }

    @Override
    protected final void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        base.getDocumentController().onScrollChanged(-1, t - oldt);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        super.onTouchEvent(ev);
        return base.getDocumentController().onTouchEvent(ev);
    }

    @Override
    public final void scrollTo(final int x, final int y) {
        final Runnable r = new Runnable() {

            @Override
            public void run() {
                final Rect l = base.getDocumentController().getScrollLimits();
                BaseDocumentView.super.scrollTo(MathUtils.adjust(x, l.left, l.right),
                        MathUtils.adjust(y, l.top, l.bottom));
            }
        };

        base.getActivity().runOnUiThread(r);
    }

    public final RectF getViewRect() {
        return new RectF(getScrollX(), getScrollY(), getScrollX() + getWidth(), getScrollY() + getHeight());
    }

    public void changeLayoutLock(final boolean lock) {
        layoutLocked = lock;
    }

    private final AtomicReference<Rect> layout = new AtomicReference<Rect>();

    private final Flag layoutFlag = new Flag();

    @Override
    protected final void onLayout(final boolean layoutChanged, final int left, final int top, final int right,
            final int bottom) {
        super.onLayout(layoutChanged, left, top, right, bottom);

        Rect oldLayout = layout.getAndSet(new Rect(left, top, right, bottom));
        base.getDocumentController().onLayoutChanged(layoutChanged, layoutLocked, oldLayout, layout.get());

        if (oldLayout == null) {
            layoutFlag.set();
        }
    }

    public final void waitForInitialization() {
        while (!layoutFlag.get()) {
            layoutFlag.waitFor(TimeUnit.SECONDS, 1);
        }
    }

    public float getScrollScaleRatio() {
        final Page page = getBase().getDocumentModel().getCurrentPageObject();
        final float zoom = getBase().getZoomModel().getZoom();

        if (page == null || page.getBounds(zoom) == null) {
            return 0;
        }
        return getWidth() * zoom / page.getBounds(zoom).width();
    }

    public void stopScroller() {
        if (!scroller.isFinished()) {
            scroller.abortAnimation();
        }
    }

    public final void redrawView() {
        redrawView(new ViewState(base.getDocumentController()));
    }

    public final void redrawView(final ViewState viewState) {
        if (viewState != null) {
            if (drawThread != null) {
                drawThread.draw(viewState);
            }
            base.getDecodeService().updateViewState(viewState);
            postInvalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        DrawTask task = drawThread.takeTask(1, TimeUnit.MILLISECONDS);
        if (task == null) {
            task = new DrawTask(new ViewState(base.getDocumentController()));
        }
        drawThread.performDrawing(canvas, task);
    }
}
