package org.ebookdroid.core;

import org.ebookdroid.core.curl.PageAnimationType;
import org.ebookdroid.core.curl.PageAnimator;
import org.ebookdroid.core.curl.PageAnimatorProxy;
import org.ebookdroid.core.curl.SinglePageView;
import org.ebookdroid.core.hwa.IHardwareAcceleration;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.settings.books.BookSettings;
import org.ebookdroid.core.touch.DefaultGestureDetector;
import org.ebookdroid.core.touch.IGestureDetector;
import org.ebookdroid.core.touch.IMultiTouchZoom;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.List;

/**
 * The Class SinglePageDocumentView.
 *
 * Used in single page view mode
 */
public class SinglePageDocumentView extends AbstractDocumentView {

    /** The curler. */
    private final PageAnimatorProxy curler = new PageAnimatorProxy(new SinglePageView(this));

    /**
     * Instantiates a new single page document view.
     *
     * @param baseActivity
     *            the base activity
     */
    public SinglePageDocumentView(final IViewerActivity baseActivity) {
        super(baseActivity);
        updateAnimationType();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.AbstractDocumentView#goToPageImpl(int)
     */
    @Override
    public final void goToPageImpl(final int toPage) {
        final DocumentModel dm = getBase().getDocumentModel();
        if (toPage >= 0 && toPage < dm.getPageCount()) {
            final Page page = dm.getPageObject(toPage);
            dm.setCurrentPageIndex(page.index);
            curler.setViewDrawn(false);
            curler.resetPageIndexes(page.index.viewIndex);
            final ViewState viewState = updatePageVisibility(page.index.viewIndex, 0, getBase().getZoomModel()
                    .getZoom());
            view.redrawView(viewState);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.AbstractDocumentView#goToPageImpl(int, float, float)
     */
    @Override
    protected void goToPageImpl(final int toPage, final float offsetX, final float offsetY) {
        final DocumentModel dm = getBase().getDocumentModel();
        if (toPage >= 0 && toPage < dm.getPageCount()) {
            final Page page = dm.getPageObject(toPage);
            dm.setCurrentPageIndex(page.index);
            curler.setViewDrawn(false);
            curler.resetPageIndexes(page.index.viewIndex);
            final ViewState viewState = updatePageVisibility(page.index.viewIndex, 0, getBase().getZoomModel()
                    .getZoom());

            final RectF bounds = page.getBounds(getBase().getZoomModel().getZoom());
            final float left = bounds.left + offsetX * bounds.width();
            final float top = bounds.top + offsetY * bounds.height();
            // if (LCTX.isDebugEnabled()) {
            // LCTX.d("goToPageImpl(): Scroll to: " + page.index.viewIndex + left + ", " + top);
            // }
            view.scrollTo((int) left, (int) top);

            view.redrawView(viewState);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#onScrollChanged(int, int)
     */
    @Override
    public void onScrollChanged(final int newPage, final int direction) {
        // bounds could be not updated
        if (inZoom.get()) {
            return;
        }
        // if (LCTX.isDebugEnabled()) {
        // LCTX.d("onScrollChanged(" + newPage + ", " + direction + ")");
        // }
        final ViewState viewState = updatePageVisibility(newPage, direction, getBase().getZoomModel().getZoom());
        DocumentModel dm = base.getDocumentModel();
        if (dm != null) {
            updatePosition(dm, dm.getCurrentPageObject(), viewState);
        }
        view.redrawView(viewState);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#calculateCurrentPage(org.ebookdroid.core.ViewState)
     */
    @Override
    public final int calculateCurrentPage(final ViewState viewState) {
        return getBase().getDocumentModel().getCurrentViewPageIndex();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#verticalConfigScroll(int)
     */
    @Override
    public final void verticalConfigScroll(final int direction) {
        curler.animate(direction);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#getScrollLimits()
     */
    @Override
    public final Rect getScrollLimits() {
        final int width = getWidth();
        final int height = getHeight();
        final float zoom = getBase().getZoomModel().getZoom();

        final DocumentModel dm = getBase().getDocumentModel();
        final Page page = dm != null ? dm.getCurrentPageObject() : null;

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
     * @see org.ebookdroid.core.AbstractDocumentView#initGestureDetectors(java.util.List)
     */
    @Override
    protected List<IGestureDetector> initGestureDetectors(final List<IGestureDetector> list) {
        list.add(IMultiTouchZoom.Factory.createImpl(base.getMultiTouchListener()));
        list.add(curler);
        list.add(new DefaultGestureDetector(base.getContext(), new GestureListener()));
        return list;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#drawView(android.graphics.Canvas, org.ebookdroid.core.ViewState)
     */
    @Override
    public final void drawView(final Canvas canvas, final ViewState viewState) {
        curler.draw(canvas, viewState);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#invalidatePageSizes(org.ebookdroid.core.IDocumentViewController.InvalidateSizeReason, org.ebookdroid.core.Page)
     */
    @Override
    public final void invalidatePageSizes(final InvalidateSizeReason reason, final Page changedPage) {
        if (!isShown()) {
            return;
        }
        if (reason == InvalidateSizeReason.ZOOM) {
            return;
        }

        final int width = getWidth();
        final int height = getHeight();

        if (changedPage == null) {
            for (final Page page : getBase().getDocumentModel().getPages()) {
                invalidatePageSize(page, width, height);
            }
        } else {
            invalidatePageSize(changedPage, width, height);
        }

        curler.setViewDrawn(false);
    }

    private void invalidatePageSize(final Page page, final int width, final int height) {
        final BookSettings bookSettings = SettingsManager.getBookSettings();
        if (bookSettings == null) {
            return;
        }
        PageAlign effectiveAlign = bookSettings.pageAlign;
        if (effectiveAlign == null) {
            effectiveAlign = PageAlign.WIDTH;
        } else if (effectiveAlign == PageAlign.AUTO) {
            final float pageHeight = width / page.getAspectRatio();
            effectiveAlign = pageHeight > height ? PageAlign.HEIGHT : PageAlign.WIDTH;
        }

        if (effectiveAlign == PageAlign.WIDTH) {
            final float pageHeight = width / page.getAspectRatio();
            page.setBounds(new RectF(0, 0, width, pageHeight));
        } else {
            final float pageWidth = height * page.getAspectRatio();
            final float widthDelta = (width - pageWidth) / 2;
            page.setBounds(new RectF(widthDelta, 0, pageWidth + widthDelta, height));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.AbstractDocumentView#isPageVisibleImpl(org.ebookdroid.core.Page, org.ebookdroid.core.ViewState)
     */
    @Override
    protected final boolean isPageVisibleImpl(final Page page, final ViewState viewState) {
        return curler.isPageVisible(page, viewState);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#updateAnimationType()
     */
    @Override
    public final void updateAnimationType() {
        final PageAnimationType animationType = SettingsManager.getBookSettings().animationType;
        final PageAnimator newCurler = PageAnimationType.create(animationType, this);

        IHardwareAcceleration.Factory.getInstance().setMode(getView().getView(), animationType.isHardwareAccelSupported());

        newCurler.init();
        curler.switchCurler(newCurler);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#pageUpdated(int)
     */
    @Override
    public void pageUpdated(final int viewIndex) {
        curler.pageUpdated(viewIndex);
    }
}
