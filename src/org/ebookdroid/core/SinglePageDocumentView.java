package org.ebookdroid.core;

import org.ebookdroid.core.curl.PageAnimationType;
import org.ebookdroid.core.curl.PageAnimator;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.settings.SettingsManager;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;

/**
 * The Class SinglePageDocumentView.
 *
 * Used in single page view mode
 */
public class SinglePageDocumentView extends AbstractDocumentView {

    /** The curler. */
    private PageAnimator curler;

    /**
     * Instantiates a new single page document view.
     *
     * @param baseActivity
     *            the base activity
     */
    public SinglePageDocumentView(final BaseViewerActivity baseActivity) {
        super(baseActivity);
        updateAnimationType();
    }

    @Override
    public final void goToPageImpl(final int toPage) {
        final DocumentModel dm = getBase().getDocumentModel();
        if (toPage >= 0 && toPage < dm.getPageCount()) {
            final Page page = dm.getPageObject(toPage);
            dm.setCurrentPageIndex(page.index);
            if (curler != null) {
                curler.setViewDrawn(false);
                curler.resetPageIndexes(page.index.viewIndex);
            }
            final ViewState viewState = updatePageVisibility(page.index.viewIndex, 0, getBase().getZoomModel()
                    .getZoom());
            redrawView(viewState);
        }
    }

    @Override
    public final int calculateCurrentPage(final ViewState viewState) {
        return getBase().getDocumentModel().getCurrentViewPageIndex();
    }

    @Override
    protected final void verticalConfigScroll(final int direction) {
        goToPageImpl(getBase().getDocumentModel().getCurrentViewPageIndex() + direction);
    }

    @Override
    protected final void verticalDpadScroll(final int direction) {
        goToPageImpl(getBase().getDocumentModel().getCurrentViewPageIndex() + direction);
    }

    @Override
    protected final Rect getScrollLimits() {
        final int width = getWidth();
        final int height = getHeight();
        final float zoom = getBase().getZoomModel().getZoom();

        final RectF bounds = getBase().getDocumentModel().getCurrentPageObject().getBounds(zoom);
        final int top = ((int) bounds.top > 0) ? 0 : (int) bounds.top;
        final int left = ((int) bounds.left > 0) ? 0 : (int) bounds.left;
        final int bottom = ((int) bounds.bottom < height) ? 0 : (int) bounds.bottom - height;
        final int right = ((int) bounds.right < width) ? 0 : (int) bounds.right - width;

        return new Rect(left, top, right, bottom);
    }

    @Override
    public final boolean onTouchEvent(final MotionEvent event) {
        if (isCurlerDisabled()) {
            return super.onTouchEvent(event);
        } else {
            if (getBase().getMultiTouchZoom() != null) {
                if (getBase().getMultiTouchZoom().onTouchEvent(event)) {
                    return true;
                }
            }

            if (!curler.handleTouchEvent(event)) {
                return super.onTouchEvent(event);
            } else {
                return true;
            }
        }
    }

    private boolean isCurlerDisabled() {
        if (curler == null) {
            return true;
        }
        final PageAlign align = getAlign();
        final float zoom = getBase().getZoomModel().getZoom();
        return align != PageAlign.AUTO || zoom != 1.0f;
    }

    @Override
    public final void drawView(final Canvas canvas, final ViewState viewState) {
        if (isCurlerDisabled()) {
            final Page page = getBase().getDocumentModel().getCurrentPageObject();
            if (page != null) {
                page.draw(canvas, viewState);
            }
        } else {
            curler.draw(canvas, viewState);
        }
    }

    /**
     * Invalidate page sizes.
     */
    @Override
    public final void invalidatePageSizes(final InvalidateSizeReason reason, final Page changedPage) {
        if (!isInitialized()) {
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

        if (curler != null) {
            curler.setViewDrawn(false);
        }

    }

    private void invalidatePageSize(final Page page, final int width, final int height) {
        PageAlign effectiveAlign = getAlign();
        if (getAlign() == PageAlign.AUTO) {
            final float pageHeight = width / page.getAspectRatio();
            if (pageHeight > height) {
                effectiveAlign = PageAlign.HEIGHT;
            } else {
                effectiveAlign = PageAlign.WIDTH;
            }
        }

        if (effectiveAlign == PageAlign.WIDTH) {
            final float pageHeight = width / page.getAspectRatio();
            final float heightDelta = (height - pageHeight) / 2;
            page.setBounds(new RectF(0, heightDelta, width, pageHeight + heightDelta));
        } else {
            final float pageWidth = height * page.getAspectRatio();
            final float widthDelta = (width - pageWidth) / 2;
            page.setBounds(new RectF(widthDelta, 0, pageWidth + widthDelta, height));
        }
    }

    @Override
    protected final boolean isPageVisibleImpl(final Page page, final ViewState viewState) {
        final int pageIndex = page.index.viewIndex;
        if (curler != null) {
            return pageIndex == curler.getForeIndex() || pageIndex == curler.getBackIndex();
        }
        return pageIndex == calculateCurrentPage(viewState);
    }

    @Override
    public final void updateAnimationType() {
        final PageAnimationType type = SettingsManager.getBookSettings().getAnimationType();
        curler = PageAnimationType.create(type, this);

        if (curler != null) {
            curler.init();
        }
    }

}
