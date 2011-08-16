package org.ebookdroid.core;

import org.ebookdroid.core.curl.PageAnimationType;
import org.ebookdroid.core.curl.PageAnimator;
import org.ebookdroid.core.utils.AndroidVersion;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

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
    public void goToPageImpl(final int toPage) {
        if (toPage >= 0 && toPage <= getBase().getDocumentModel().getPageCount()) {
            final Page page = getBase().getDocumentModel().getPageObject(toPage);
            getBase().getDocumentModel().setCurrentPageIndex(page.getDocumentPageIndex(), page.getIndex());
            if (curler != null) {
                curler.resetPageIndexes();
            }
            updatePageVisibility();
        }
//        redrawView();
    }

    @Override
    public int getCurrentPage() {
        return getBase().getDocumentModel().getCurrentViewPageIndex();
    }

    @Override
    protected void verticalConfigScroll(final int direction) {
        goToPageImpl(getBase().getDocumentModel().getCurrentViewPageIndex() + direction);
    }

    @Override
    protected void verticalDpadScroll(final int direction) {
        goToPageImpl(getBase().getDocumentModel().getCurrentViewPageIndex() + direction);
    }

    @Override
    protected int getTopLimit() {
        final RectF bounds = getBase().getDocumentModel().getCurrentPageObject().getBounds();
        return ((int) bounds.top > 0) ? 0 : (int) bounds.top;
    }

    @Override
    protected int getLeftLimit() {
        final RectF bounds = getBase().getDocumentModel().getCurrentPageObject().getBounds();
        return ((int) bounds.left > 0) ? 0 : (int) bounds.left;
    }

    @Override
    protected int getBottomLimit() {
        final RectF bounds = getBase().getDocumentModel().getCurrentPageObject().getBounds();
        return ((int) bounds.bottom < getHeight()) ? 0 : (int) bounds.bottom - getHeight();
    }

    @Override
    protected int getRightLimit() {
        final RectF bounds = getBase().getDocumentModel().getCurrentPageObject().getBounds();
        return ((int) bounds.right < getWidth()) ? 0 : (int) bounds.right - getWidth();
    }

    @Override
    public void scrollTo(final int x, final int y) {
        super.scrollTo(Math.min(Math.max(x, getLeftLimit()), getRightLimit()),
                Math.min(Math.max(y, getTopLimit()), getBottomLimit()));
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (isCurlerDisabled()) {
            return super.onTouchEvent(event);
        } else {
            if (getBase().getMultiTouchZoom() != null) {
                if (getBase().getMultiTouchZoom().onTouchEvent(event)) {
                    return true;
                }
                if (getBase().getMultiTouchZoom().isResetLastPointAfterZoom()) {
                    setLastPosition(event);
                    getBase().getMultiTouchZoom().setResetLastPointAfterZoom(false);
                }
            }

            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain();
            }
            velocityTracker.addMovement(event);

            return curler.handleTouchEvent(event);
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
    public void drawView(final Canvas canvas, RectF viewRect) {
        if (isCurlerDisabled()) {
            final Page page = getBase().getDocumentModel().getCurrentPageObject();
            if (page != null) {
                page.draw(canvas, viewRect);
            }
        } else {
            curler.draw(canvas, viewRect);
        }
    }
    /**
     * Invalidate page sizes.
     */
    @Override
    public void invalidatePageSizes() {
        if (!isInitialized()) {
            return;
        }
        final int width = getWidth();
        final int height = getHeight();
        final float zoom = getBase().getZoomModel().getZoom();

        for (int i = 0; i < getBase().getDocumentModel().getPages().size(); i++) {
            final Page page = getBase().getDocumentModel().getPages().get(i);

            PageAlign effectiveAlign = getAlign();
            if (getAlign() == PageAlign.AUTO) {
                final float pageHeight = page.getPageHeight(width, zoom);
                if (pageHeight > height) {
                    effectiveAlign = PageAlign.HEIGHT;
                } else {
                    effectiveAlign = PageAlign.WIDTH;
                }
            }

            if (effectiveAlign == PageAlign.WIDTH) {
                final float pageHeight = page.getPageHeight(width, zoom);
                page.setBounds(new RectF(0, ((height - pageHeight) / 2), width * zoom, pageHeight
                        + ((height - pageHeight) / 2)));
            } else {
                final float pageWidth = page.getPageWidth(height, zoom);
                page.setBounds(new RectF((width - pageWidth) / 2, 0, pageWidth + (width - pageWidth) / 2, height * zoom));
            }
        }
        if (curler != null) {
            curler.setViewDrawn(false);
        }
    }

    @Override
    public boolean isPageVisible(final Page page) {
        return page.getIndex() == getCurrentPage();
    }

    @Override
    public void updateAnimationType() {
        final PageAnimationType type = getBase().getBookSettings().getAnimationType();
        curler = PageAnimationType.create(type, this);

        if (!AndroidVersion.lessThan3x) {
            final int layerType = type.isHardwareAccelSupported() ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_SOFTWARE;
            this.setLayerType(layerType, null);
        }

        if (curler != null) {
            curler.init();
        }
    }


}
