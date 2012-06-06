package org.ebookdroid.core;

import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.common.touch.DefaultGestureDetector;
import org.ebookdroid.common.touch.IGestureDetector;
import org.ebookdroid.common.touch.MultiTouchGestureDetectorFactory;
import org.ebookdroid.core.curl.PageAnimationType;
import org.ebookdroid.core.curl.PageAnimator;
import org.ebookdroid.core.curl.PageAnimatorProxy;
import org.ebookdroid.core.curl.SinglePageView;
import org.ebookdroid.ui.viewer.IActivityController;

import android.graphics.Rect;
import android.graphics.RectF;

import java.util.List;

import org.emdev.ui.uimanager.IUIManager;
import org.emdev.utils.LengthUtils;

/**
 * The Class SinglePageController.
 * 
 * Used in single page view mode
 */
public class SinglePageController extends AbstractViewController {

    /** The curler. */
    private final PageAnimatorProxy curler = new PageAnimatorProxy(new SinglePageView(this));

    /**
     * Instantiates a new single page document view.
     * 
     * @param baseActivity
     *            the base activity
     */
    public SinglePageController(final IActivityController baseActivity) {
        super(baseActivity, DocumentViewMode.SINGLE_PAGE);
        updateAnimationType();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.AbstractViewController#goToPageImpl(int)
     */
    @Override
    public final ViewState goToPage(final int toPage) {
        if (toPage >= 0 && toPage < model.getPageCount()) {
            final Page page = model.getPageObject(toPage);
            model.setCurrentPageIndex(page.index);
            curler.setViewDrawn(false);
            curler.resetPageIndexes(page.index.viewIndex);

            final ViewState viewState = EventPool.newEventScrollTo(this, page.index.viewIndex).process();
            getView().redrawView(viewState);
            return viewState;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.AbstractViewController#goToPageImpl(int, float, float)
     */
    @Override
    public ViewState goToPage(final int toPage, final float offsetX, final float offsetY) {
        if (toPage >= 0 && toPage < model.getPageCount()) {
            final Page page = model.getPageObject(toPage);
            model.setCurrentPageIndex(page.index);
            curler.setViewDrawn(false);
            curler.resetPageIndexes(page.index.viewIndex);

            final RectF bounds = page.getBounds(getBase().getZoomModel().getZoom());
            final float left = bounds.left + offsetX * bounds.width();
            final float top = bounds.top + offsetY * bounds.height();
            getView().scrollTo((int) left, (int) top);

            final ViewState viewState = EventPool.newEventScrollTo(this, page.index.viewIndex).process();
            pageUpdated(viewState, page);
            getView().redrawView(viewState);
            return viewState;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#onScrollChanged(int, int)
     */
    @Override
    public void onScrollChanged(final int dX, final int dY) {
        // bounds could be not updated
        if (inZoom.get()) {
            return;
        }

        EventPool.newEventScroll(this, dX).process();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#calculateCurrentPage(org.ebookdroid.core.ViewState)
     */
    @Override
    public final int calculateCurrentPage(final ViewState viewState, final int firstVisible, final int lastVisible) {
        return viewState.model.getCurrentViewPageIndex();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#verticalConfigScroll(int)
     */
    @Override
    public final void verticalConfigScroll(final int direction) {
        if (curler.enabled()) {
            curler.animate(direction);
        } else {
            final BookSettings bs = SettingsManager.getBookSettings();
            final float offsetX = bs != null ? bs.offsetX : 0;

            final Page page = model.getCurrentPageObject();
            final RectF viewRect = base.getView().getViewRect();
            final RectF bounds = page.getBounds(getBase().getZoomModel().getZoom());

            if (Math.abs(viewRect.top - bounds.top) < 5 && direction < 0) {
                goToPage(page.index.viewIndex - 1, offsetX, 1);
                return;
            }

            if (Math.abs(viewRect.bottom - bounds.bottom) < 5 && direction > 0) {
                goToPage(page.index.viewIndex + 1, offsetX, 0);
                return;
            }

            final float pageHeight = bounds.height();
            final float viewHeight = viewRect.height();

            final float diff = direction * viewHeight * AppSettings.current().scrollHeight / 100.0f;
            final float oldTop = getScrollY();
            final float newTop = oldTop + diff;

            goToPage(model.getCurrentViewPageIndex(), offsetX, newTop / pageHeight);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#getScrollLimits()
     */
    @Override
    public final Rect getScrollLimits() {
        final int width = getWidth();
        final int height = getHeight();
        final float zoom = getBase().getZoomModel().getZoom();

        final Page page = model.getCurrentPageObject();

        if (page != null) {
            final RectF bounds = page.getBounds(zoom);
            final int top = ((int) bounds.top > 0) ? 0 : (int) bounds.top;
            final int left = ((int) bounds.left > 0) ? 0 : (int) bounds.left;
            final int bottom = ((int) bounds.bottom < height) ? 0 : (int) bounds.bottom - height;
            final int right = ((int) bounds.right < width) ? 0 : (int) bounds.right - width;

            return new Rect(left, top, right, bottom);
        }

        return new Rect(0, 0, 0, 0);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.AbstractViewController#initGestureDetectors(java.util.List)
     */
    @Override
    protected List<IGestureDetector> initGestureDetectors(final List<IGestureDetector> list) {
        final GestureListener listener = new GestureListener();
        list.add(MultiTouchGestureDetectorFactory.create(listener));
        list.add(curler);
        list.add(new DefaultGestureDetector(base.getContext(), listener));
        return list;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#drawView(org.ebookdroid.core.EventDraw)
     */
    @Override
    public void drawView(final EventDraw eventDraw) {
        curler.draw(eventDraw);
    }

    public final ViewState invalidatePages(final ViewState oldState, final Page... pages) {
        if (LengthUtils.isNotEmpty(pages) && pages[0] != null) {
            return EventPool.newEventScrollTo(this, pages[0].index.viewIndex).process();
        }
        return oldState;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#invalidatePageSizes(org.ebookdroid.ui.viewer.IViewController.InvalidateSizeReason,
     *      org.ebookdroid.core.Page)
     */
    @Override
    public final void invalidatePageSizes(final InvalidateSizeReason reason, final Page changedPage) {
        if (!isShown()) {
            return;
        }
        final int width = getWidth();
        final int height = getHeight();
        final BookSettings bookSettings = SettingsManager.getBookSettings();
        final PageAlign pageAlign = DocumentViewMode.getPageAlign(bookSettings);

        if (changedPage == null) {
            for (final Page page : model.getPages()) {
                invalidatePageSize(pageAlign, page, width, height);
            }
        } else {
            invalidatePageSize(pageAlign, changedPage, width, height);
        }

        curler.setViewDrawn(false);
    }

    private void invalidatePageSize(final PageAlign pageAlign, final Page page, final int width, final int height) {
        final RectF pageBounds = calcPageBounds(pageAlign, page.getAspectRatio(), width, height);
        final float pageWidth = pageBounds.width();
        if (width > pageWidth) {
            final float widthDelta = (width - pageWidth) / 2;
            pageBounds.offset(widthDelta, 0);
        }
        page.setBounds(pageBounds);
    }

    @Override
    public RectF calcPageBounds(final PageAlign pageAlign, final float pageAspectRatio, final int width,
            final int height) {
        PageAlign effective = pageAlign;
        if (effective == PageAlign.AUTO) {
            final float pageHeight = width / pageAspectRatio;
            effective = pageHeight > height ? PageAlign.HEIGHT : PageAlign.WIDTH;
        }

        if (effective == PageAlign.WIDTH) {
            final float pageHeight = width / pageAspectRatio;
            return new RectF(0, 0, width, pageHeight);
        } else {
            final float pageWidth = height * pageAspectRatio;
            return new RectF(0, 0, pageWidth, height);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.AbstractViewController#isPageVisible(org.ebookdroid.core.Page,
     *      org.ebookdroid.core.ViewState)
     */
    @Override
    public final boolean isPageVisible(final Page page, final ViewState viewState) {
        return curler.isPageVisible(page, viewState);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#updateAnimationType()
     */
    @Override
    public final void updateAnimationType() {
        final PageAnimationType animationType = SettingsManager.getBookSettings().animationType;
        final PageAnimator newCurler = PageAnimationType.create(animationType, this);

        IUIManager.instance.setHardwareAccelerationEnabled(AppSettings.current().hwaEnabled);
        IUIManager.instance.setHardwareAccelerationMode(getView().getView(), animationType.isHardwareAccelSupported());

        newCurler.init();
        curler.switchCurler(newCurler);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.AbstractViewController#pageUpdated(org.ebookdroid.core.ViewState,
     *      org.ebookdroid.core.Page)
     */
    @Override
    public void pageUpdated(final ViewState viewState, final Page page) {
        curler.pageUpdated(viewState, page);
    }
}
