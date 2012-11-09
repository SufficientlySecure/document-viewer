package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.core.models.DocumentModel.PageIterator;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.views.DragMark;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;

import org.emdev.ui.uimanager.IUIManager;

public abstract class AbstractScrollController extends AbstractViewController {

    protected static volatile Bitmap dragBitmap;

    protected AbstractScrollController(final IActivityController base, final DocumentViewMode mode) {
        super(base, mode);
        if (dragBitmap == null) {
            dragBitmap = BitmapFactory.decodeResource(base.getContext().getResources(), R.drawable.components_curler_drag);
        }
        IUIManager.instance.setHardwareAccelerationEnabled(base.getActivity(), AppSettings.current().hwaEnabled);
        IUIManager.instance.setHardwareAccelerationMode(base.getActivity(), getView().getView(), true);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#goToPage(int)
     */
    @Override
    public final ViewState goToPage(final int toPage) {
        return new EventGotoPage(this, toPage).process();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#goToPage(int, float, float)
     */
    @Override
    public final ViewState goToPage(final int toPage, final float offsetX, final float offsetY) {
        return new EventGotoPage(this, toPage, offsetX, offsetY).process();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#drawView(org.ebookdroid.core.EventDraw)
     */
    @Override
    public final void drawView(final EventDraw eventDraw) {
        final ViewState viewState = eventDraw.viewState;
        if (viewState.model == null) {
            return;
        }

        final PageIterator pages = viewState.pages.getVisiblePages();
        try {
            for (final Page page : pages) {
                if (page != null) {
                    eventDraw.process(page);
                }
            }
        } finally {
            pages.release();
        }

        if (eventDraw.viewState.app.showAnimIcon) {
            DragMark.draw(eventDraw.canvas, viewState);
        }
        getView().continueScroll();
    }

    @Override
    public final void drawView(final EventGLDraw eventDraw) {
        final ViewState viewState = eventDraw.viewState;
        if (viewState.model == null) {
            return;
        }

        final PageIterator pages = viewState.pages.getVisiblePages();
        try {
            for (final Page page : pages) {
                if (page != null) {
                    eventDraw.process(page);
                }
            }
        } finally {
            pages.release();
        }

        // if (eventDraw.viewState.app.showAnimIcon) {
        // DragMark.draw(eventDraw.canvas, viewState);
        // }
        getView().continueScroll();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.AbstractViewController#onLayoutChanged(boolean, boolean, android.graphics.Rect,
     *      android.graphics.Rect)
     */
    @Override
    public final boolean onLayoutChanged(final boolean layoutChanged, final boolean layoutLocked, final Rect oldLaout,
            final Rect newLayout) {
        final BookSettings bs = base.getBookSettings();
        final int page = model != null ? model.getCurrentViewPageIndex() : -1;
        final float offsetX = bs != null ? bs.offsetX : 0;
        final float offsetY = bs != null ? bs.offsetY : 0;

        if (super.onLayoutChanged(layoutChanged, layoutLocked, oldLaout, newLayout)) {
            if (isShown && layoutChanged && page != -1) {
                goToPage(page, offsetX, offsetY);
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#onScrollChanged(int, int)
     */
    @Override
    public final void onScrollChanged(final int dX, final int dY) {
        if (inZoom.get()) {
            return;
        }

        EventPool.newEventScroll(this, mode == DocumentViewMode.VERTICALL_SCROLL ? dY : dX).process();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#isPageVisible(org.ebookdroid.core.Page,
     *      org.ebookdroid.core.ViewState, android.graphics.RectF)
     */
    @Override
    public final boolean isPageVisible(final Page page, final ViewState viewState, final RectF outBounds) {
        viewState.getBounds(page, outBounds);
        return RectF.intersects(viewState.viewRect, outBounds);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#pageUpdated(org.ebookdroid.core.ViewState,
     *      org.ebookdroid.core.Page)
     */
    @Override
    public void pageUpdated(final ViewState viewState, final Page page) {
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#updateAnimationType()
     */
    @Override
    public void updateAnimationType() {
    }
}
