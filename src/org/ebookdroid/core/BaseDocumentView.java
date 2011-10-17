package org.ebookdroid.core;

import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.utils.MathUtils;

import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Scroller;

public final  class BaseDocumentView extends SurfaceView implements SurfaceHolder.Callback {

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
        getHolder().addCallback(this);
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

    @Override
    protected final void onLayout(final boolean layoutChanged, final int left, final int top, final int right,
            final int bottom) {
        super.onLayout(layoutChanged, left, top, right, bottom);
        base.getDocumentController().onLayoutChanged(layoutChanged, layoutLocked, left, top, right, bottom);
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
        if (!getScroller().isFinished()) {
            getScroller().abortAnimation();
        }
    }

    @Override
    public final void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
        redrawView();
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
        }
    }

    @Override
    public final void surfaceCreated(final SurfaceHolder holder) {
        drawThread = new DrawThread(getHolder());
        drawThread.start();
    }

    @Override
    public final void surfaceDestroyed(final SurfaceHolder holder) {
        drawThread.finish();
    }
}
