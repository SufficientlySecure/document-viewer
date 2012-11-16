package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.common.keysbinding.KeyBindingsManager;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.common.touch.DefaultGestureDetector;
import org.ebookdroid.common.touch.IGestureDetector;
import org.ebookdroid.common.touch.IMultiTouchListener;
import org.ebookdroid.common.touch.MultiTouchGestureDetector;
import org.ebookdroid.common.touch.TouchManager;
import org.ebookdroid.common.touch.TouchManager.Touch;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.models.DocumentModel.PageIterator;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.IView;
import org.ebookdroid.ui.viewer.IViewController;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.FloatMath;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.ui.actions.AbstractComponentController;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.ui.actions.params.Constant;
import org.emdev.ui.progress.IProgressIndicator;
import org.emdev.utils.LengthUtils;

public abstract class AbstractViewController extends AbstractComponentController<IView> implements IViewController {

    protected static final LogContext LCTX = LogManager.root().lctx("View", false);

    public static final int DOUBLE_TAP_TIME = 500;

    private static final Float FZERO = Float.valueOf(0);

    public final IActivityController base;

    public final DocumentModel model;

    public final DocumentViewMode mode;

    protected boolean isInitialized = false;

    protected boolean isShown = false;

    protected final AtomicBoolean inZoom = new AtomicBoolean();

    protected final AtomicBoolean inQuickZoom = new AtomicBoolean();

    protected final AtomicBoolean inZoomToColumn = new AtomicBoolean();

    protected final PageIndex pageToGo;

    protected int firstVisiblePage;

    protected int lastVisiblePage;

    protected boolean layoutLocked;

    private List<IGestureDetector> detectors;

    public AbstractViewController(final IActivityController base, final DocumentViewMode mode) {
        super(base, base.getView());

        this.base = base;
        this.mode = mode;
        this.model = base.getDocumentModel();

        this.firstVisiblePage = -1;
        this.lastVisiblePage = -1;

        this.pageToGo = base.getBookSettings().getCurrentPage();

        createAction(R.id.actions_verticalConfigScrollUp, new Constant("direction", -1));
        createAction(R.id.actions_verticalConfigScrollDown, new Constant("direction", +1));
        createAction(R.id.actions_leftTopCorner, new Constant("offsetX", 0), new Constant("offsetY", 0));
        createAction(R.id.actions_leftBottomCorner, new Constant("offsetX", 0), new Constant("offsetY", 1));
        createAction(R.id.actions_rightTopCorner, new Constant("offsetX", 1), new Constant("offsetY", 0));
        createAction(R.id.actions_rightBottomCorner, new Constant("offsetX", 1), new Constant("offsetY", 1));
    }

    protected List<IGestureDetector> getGestureDetectors() {
        if (detectors == null) {
            detectors = initGestureDetectors(new ArrayList<IGestureDetector>(4));
        }
        return detectors;
    }

    protected List<IGestureDetector> initGestureDetectors(final List<IGestureDetector> list) {
        final GestureListener listener = new GestureListener();
        list.add(new MultiTouchGestureDetector(listener));
        list.add(new DefaultGestureDetector(base.getContext(), listener));
        return list;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#getView()
     */
    @Override
    public final IView getView() {
        return base.getView();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#getBase()
     */
    @Override
    public final IActivityController getBase() {
        return base;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#init(org.ebookdroid.ui.viewer.IActivityController.IBookLoadTask)
     */
    @Override
    public final void init(final IProgressIndicator task) {
        if (!isInitialized) {
            try {
                model.initPages(base, task);
            } finally {
                isInitialized = true;
            }
        }
    }

    /**
     *
     */
    @Override
    public final void onDestroy() {
        // isShown = false;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#show()
     */
    @Override
    public final void show() {
        if (!isInitialized) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("View is not initialized yet");
            }
            return;
        }
        if (!isShown) {
            isShown = true;
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Showing view content...");
            }

            invalidatePageSizes(InvalidateSizeReason.INIT, null);

            final BookSettings bs = base.getBookSettings();
            bs.lastChanged = System.currentTimeMillis();

            final Page page = pageToGo.getActualPage(model, bs);
            final int toPage = page != null ? page.index.viewIndex : 0;

            goToPage(toPage, bs.offsetX, bs.offsetY);
        } else {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("View has been shown before");
            }
        }
    }

    protected final void updatePosition(final Page page, final ViewState viewState) {
        final PointF pos = viewState.getPositionOnPage(page);
        SettingsManager.positionChanged(base.getBookSettings(), pos.x, pos.y);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.events.ZoomListener#zoomChanged(float, float, boolean)
     */
    @Override
    public final void zoomChanged(final float oldZoom, final float newZoom, final boolean committed) {
        if (!isShown) {
            return;
        }

        inZoom.set(!committed);
        EventPool.newEventZoom(this, oldZoom, newZoom, committed).process().release();

        if (committed) {
            base.getManagedComponent().zoomChanged(newZoom);
        } else {
            inQuickZoom.set(false);
            inZoomToColumn.set(false);
        }
    }

    @ActionMethod(ids = R.id.actions_quickZoom)
    public final void quickZoom(final ActionEx action) {
        if (inZoom.get()) {
            return;
        }
        float zoomFactor = 2.0f;
        if (inQuickZoom.compareAndSet(true, false)) {
            zoomFactor = 1.0f / zoomFactor;
        } else {
            inQuickZoom.set(true);
            inZoomToColumn.set(false);
        }
        base.getZoomModel().scaleAndCommitZoom(zoomFactor);
    }

    @ActionMethod(ids = R.id.actions_zoomToColumn)
    public final void zoomToColumn(final ActionEx action) {
        if (inZoom.get()) {
            return;
        }

        final int tapX = action.getParameter("tap_x", FZERO).intValue();
        final int tapY = action.getParameter("tap_y", FZERO).intValue();
        if (tapX == 0 && tapY == 0) {
            return;
        }

        // System.out.println("AbstractViewController.zoomToColumn(" + tapX + "," + tapY + ")");
        PointF pos = null;
        Page page = null;

        final ViewState vs = ViewState.get(this);
        try {
            final PageIterator pages = model.getPages(firstVisiblePage, lastVisiblePage + 1);
            try {
                for (final Page p : pages) {
                    pos = vs.getPositionOnPage(p, tapX, tapY);
                    if ((0 <= pos.x && pos.x <= 1) && (0 <= pos.y && pos.y <= 1)) {
                        page = p;
                        break;
                    }
                }
            } finally {
                pages.release();
            }
            if (page == null) {
                return;
            }

            final IView view = base.getView();
            if (inZoomToColumn.compareAndSet(true, false)) {
                base.getZoomModel().setZoom(1.0f, true);
                final float offsetX = 0;
                final float offsetY = pos.y - 0.5f * (view.getHeight() / page.getBounds(1.0f).height());
                goToPage(page.index.viewIndex, offsetX, offsetY);
                return;
            }

            final RectF column = page.getColumn(pos);
            // System.out.println("AbstractViewController.zoomToColumn(): column = " + column);

            if (column == null || column.width() > 0.95f) {
                return;
            }

            inZoomToColumn.set(true);
            inQuickZoom.set(false);

            final int screenWidth = view.getWidth();
            final int screenHeight = view.getHeight();

            final RectF pb = vs.getBounds(page);

            final float columnScreenWidth = page.getPageRegion(pb, new RectF(column)).width();

            final float newZoom = screenWidth / columnScreenWidth;

            base.getZoomModel().setZoom(newZoom, true);

            scrollToColumn(page, column, pos, screenHeight);
        } finally {
            vs.release();
        }
    }

    protected void scrollToColumn(final Page page, final RectF column, final PointF pos, final int screenHeight) {
        final ViewState vs = ViewState.get(AbstractViewController.this);
        final RectF pb = vs.getBounds(page);
        final RectF columnRegion = page.getPageRegion(pb, new RectF(column));
        columnRegion.offset(-vs.viewBase.x, -vs.viewBase.y);

        final float toX = columnRegion.left;
        final float toY = pb.top + pos.y * pb.height() - 0.5f * screenHeight;
        getView().scrollTo((int) toX, (int) toY);

        vs.release();
    }

    @ActionMethod(ids = { R.id.actions_leftTopCorner, R.id.actions_leftBottomCorner, R.id.actions_rightTopCorner,
            R.id.actions_rightBottomCorner })
    public void scrollToCorner(final ActionEx action) {
        final Integer offX = action.getParameter("offsetX");
        final Integer offY = action.getParameter("offsetY");

        final float offsetX = offX != null ? offX.floatValue() : 0;
        final float offsetY = offY != null ? offY.floatValue() : 0;

        new EventGotoPageCorner(this, offsetX, offsetY).process().release();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#updateMemorySettings()
     */
    @Override
    public final void updateMemorySettings() {
        EventPool.newEventReset(this, null, false).process().release();
    }

    public final int getScrollX() {
        return getView().getScrollX();
    }

    public final int getWidth() {
        return getView().getWidth();
    }

    public final int getScrollY() {
        return getView().getScrollY();
    }

    public final int getHeight() {
        return getView().getHeight();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#dispatchKeyEvent(android.view.KeyEvent)
     */
    @Override
    public final boolean dispatchKeyEvent(final KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            final Integer actionId = KeyBindingsManager.getAction(event);
            final ActionEx action = actionId != null ? getOrCreateAction(actionId) : null;
            if (action != null) {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("Key action: " + action.name + ", " + action.getMethod().toString());
                }
                action.run();
                return true;
            } else {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("Key action not found: " + event);
                }
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            final Integer id = KeyBindingsManager.getAction(event);
            if (id != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#onTouchEvent(android.view.MotionEvent)
     */
    @Override
    public final boolean onTouchEvent(final MotionEvent ev) {
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
     * @see org.ebookdroid.ui.viewer.IViewController#onLayoutChanged(boolean, boolean, android.graphics.Rect,
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
                EventPool.newEventReset(this, InvalidateSizeReason.LAYOUT, true).process().release();
                return true;
            } else {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("onLayoutChanged(): view not shown yet");
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#toggleRenderingEffects()
     */
    @Override
    public final void toggleRenderingEffects() {
        EventPool.newEventReset(this, null, true).process().release();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#invalidateScroll()
     */
    @Override
    public final void invalidateScroll() {
        if (!isShown) {
            return;
        }
        getView().invalidateScroll();
    }

    /**
     * Sets the page align flag.
     *
     * @param align
     *            the new flag indicating align
     */
    @Override
    public final void setAlign(final PageAlign align) {
        EventPool.newEventReset(this, InvalidateSizeReason.PAGE_ALIGN, false).process().release();
    }

    /**
     * Checks if view is initialized.
     *
     * @return true, if is initialized
     */
    protected final boolean isShown() {
        return isShown;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#getFirstVisiblePage()
     */
    @Override
    public final int getFirstVisiblePage() {
        return firstVisiblePage;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#getLastVisiblePage()
     */
    @Override
    public final int getLastVisiblePage() {
        return lastVisiblePage;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#redrawView()
     */
    @Override
    public final void redrawView() {
        getView().redrawView(ViewState.get(this));
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#redrawView(org.ebookdroid.core.ViewState)
     */
    @Override
    public final void redrawView(final ViewState viewState) {
        getView().redrawView(viewState);
    }

    @ActionMethod(ids = { R.id.actions_verticalConfigScrollUp, R.id.actions_verticalConfigScrollDown })
    public final void verticalConfigScroll(final ActionEx action) {
        final Integer direction = action.getParameter("direction");
        verticalConfigScroll(direction);
    }

    protected final boolean processTap(final TouchManager.Touch type, final MotionEvent e) {
        final float x = e.getX();
        final float y = e.getY();

        if (type == Touch.SingleTap) {
            if (processLinkTap(x, y)) {
                return true;
            }
        }

        return processActionTap(type, x, y);
    }

    protected boolean processActionTap(final TouchManager.Touch type, final float x, final float y) {
        final Integer actionId = TouchManager.getAction(type, x, y, getWidth(), getHeight());
        final ActionEx action = actionId != null ? getOrCreateAction(actionId) : null;
        if (action != null) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Touch action: " + action.name + ", " + action.getMethod().toString());
            }
            action.addParameter(new Constant("tap_x", Float.valueOf(x))).addParameter(
                    new Constant("tap_y", Float.valueOf(y)));
            action.run();
            return true;
        } else {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Touch action not found");
            }
        }
        return false;
    }

    protected final boolean processLinkTap(final float x, final float y) {
        final float zoom = base.getZoomModel().getZoom();
        final RectF rect = new RectF(x, y, x, y);
        rect.offset(getScrollX(), getScrollY());

        final PageIterator pages = model.getPages(firstVisiblePage, lastVisiblePage + 1);
        try {
            final RectF bounds = new RectF();
            for (final Page page : pages) {
                page.getBounds(zoom, bounds);
                if (RectF.intersects(bounds, rect)) {
                    if (LengthUtils.isNotEmpty(page.links)) {
                        for (final PageLink link : page.links) {
                            if (processLinkTap(page, link, bounds, rect)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            }
        } finally {
            pages.release();
        }
        return false;
    }

    protected final boolean processLinkTap(final Page page, final PageLink link, final RectF pageBounds,
            final RectF tapRect) {
        final RectF linkRect = page.getLinkSourceRect(pageBounds, link);
        if (linkRect == null || !RectF.intersects(linkRect, tapRect)) {
            return false;
        }

        if (LCTX.isDebugEnabled()) {
            LCTX.d("Page link found under tap: " + link);
        }

        goToLink(link.targetPage, link.targetRect, AppSettings.current().storeLinkGotoHistory);
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#goToLink(int, android.graphics.RectF)
     */
    @Override
    public void goToLink(final int pageDocIndex, final RectF targetRect, final boolean addToHistory) {
        if (pageDocIndex >= 0) {
            final PointF linkPoint = new PointF();
            final Page target = model.getLinkTargetPage(pageDocIndex, targetRect, linkPoint,
                    base.getBookSettings().splitRTL);
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Target page found: " + target);
            }
            if (target != null) {
                base.jumpToPage(target.index.viewIndex, linkPoint.x, linkPoint.y, addToHistory);
            }
        }
    }

    protected class GestureListener extends SimpleOnGestureListener implements IMultiTouchListener {

        protected final LogContext LCTX = LogManager.root().lctx("Gesture", false);

        /**
         * {@inheritDoc}
         *
         * @see android.view.GestureDetector.SimpleOnGestureListener#onDoubleTap(android.view.MotionEvent)
         */
        @Override
        public boolean onDoubleTap(final MotionEvent e) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("onDoubleTap(" + e + ")");
            }
            return processTap(TouchManager.Touch.DoubleTap, e);
        }

        /**
         * {@inheritDoc}
         *
         * @see android.view.GestureDetector.SimpleOnGestureListener#onDown(android.view.MotionEvent)
         */
        @Override
        public boolean onDown(final MotionEvent e) {
            getView().forceFinishScroll();
            if (LCTX.isDebugEnabled()) {
                LCTX.d("onDown(" + e + ")");
            }
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
            if (LCTX.isDebugEnabled()) {
                LCTX.d("onFling(" + x + ", " + y + ")");
            }
            getView().startFling(x, y, l);
            getView().redrawView();
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
            if (LCTX.isDebugEnabled()) {
                LCTX.d("onScroll(" + x + ", " + y + ")");
            }
            getView().scrollBy((int) x, (int) y);
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @see android.view.GestureDetector.SimpleOnGestureListener#onSingleTapUp(android.view.MotionEvent)
         */
        @Override
        public boolean onSingleTapUp(final MotionEvent e) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("onSingleTapUp(" + e + ")");
            }
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @see android.view.GestureDetector.SimpleOnGestureListener#onSingleTapConfirmed(android.view.MotionEvent)
         */
        @Override
        public boolean onSingleTapConfirmed(final MotionEvent e) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("onSingleTapConfirmed(" + e + ")");
            }
            return processTap(TouchManager.Touch.SingleTap, e);
        }

        /**
         * {@inheritDoc}
         *
         * @see android.view.GestureDetector.SimpleOnGestureListener#onLongPress(android.view.MotionEvent)
         */
        @Override
        public void onLongPress(final MotionEvent e) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("onLongPress(" + e + ")");
            }
            // LongTap operation cause side-effects
            // processTap(TouchManager.Touch.LongTap, e);
        }

        /**
         * {@inheritDoc}
         *
         * @see org.ebookdroid.common.touch.IMultiTouchListener#onTwoFingerPinch(float, float)
         */
        @Override
        public void onTwoFingerPinch(final MotionEvent e, final float oldDistance, final float newDistance) {
            final float factor = FloatMath.sqrt(newDistance / oldDistance);
            if (LCTX.isDebugEnabled()) {
                LCTX.d("onTwoFingerPinch(" + oldDistance + ", " + newDistance + "): " + factor);
            }
            base.getZoomModel().scaleZoom(factor);
        }

        /**
         * {@inheritDoc}
         *
         * @see org.ebookdroid.common.touch.IMultiTouchListener#onTwoFingerPinchEnd()
         */
        @Override
        public void onTwoFingerPinchEnd(final MotionEvent e) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("onTwoFingerPinch(" + e + ")");
            }
            base.getZoomModel().commit();
        }

        /**
         * {@inheritDoc}
         *
         * @see org.ebookdroid.common.touch.IMultiTouchListener#onTwoFingerTap()
         */
        @Override
        public void onTwoFingerTap(final MotionEvent e) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("onTwoFingerTap(" + e + ")");
            }
            processTap(TouchManager.Touch.TwoFingerTap, e);
        }
    }
}
