package org.ebookdroid.core;

import org.ebookdroid.core.curl.PageAnimationType;
import org.ebookdroid.core.curl.PageAnimator;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.utils.CompareUtils;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.VelocityTracker;

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
        final DocumentModel dm = getBase().getDocumentModel();
        if (toPage >= 0 && toPage < dm.getPageCount()) {
            final Page page = dm.getPageObject(toPage);
            if (page != null) {
                dm.setCurrentPageIndex(page.getDocumentPageIndex(), page.getIndex());
                updatePageVisibility(page.getIndex(), 0);
            }
            if (curler != null) {
                curler.resetPageIndexes();
            }
        }
    }

    @Override
    public int getCurrentPage() {
        return getBase().getDocumentModel().getCurrentViewPageIndex();
    }

    @Override
    public int compare(final PageTreeNode node1, final PageTreeNode node2) {
        final RectF viewRect = getViewRect();
        final RectF rect1 = node1.getTargetRect(viewRect, node1.page.getBounds());
        final RectF rect2 = node1.getTargetRect(viewRect, node2.page.getBounds());

        final int cp = getCurrentPage();

        if (node1.page.index == cp && node2.page.index == cp) {
            int res = CompareUtils.compare(rect1.top, rect2.top);
            if (res == 0) {
                res = CompareUtils.compare(rect1.left, rect2.left);
            }
            return res;
        }

        final int dist1 = Math.abs(node1.page.index - cp);
        final int dist2 = Math.abs(node2.page.index - cp);

        return CompareUtils.compare(dist1, dist2);
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
    public void drawView(final Canvas canvas, final RectF viewRect) {
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
    public void invalidatePageSizes(final InvalidateSizeReason reason, final Page changedPage) {
        if (!isInitialized()) {
            return;
        }
        final int width = getWidth();
        final int height = getHeight();
        final float zoom = getBase().getZoomModel().getZoom();

        if (changedPage == null) {
            for (final Page page : getBase().getDocumentModel().getPages()) {
                invalidatePageSize(page, zoom, width, height);
            }
        } else {
            invalidatePageSize(changedPage, zoom, width, height);
        }

        if (curler != null) {
            curler.setViewDrawn(false);
        }

    }

    private void invalidatePageSize(final Page page, final float zoom, final int width, final int height) {
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
            final float heightDelta = (height - pageHeight) / 2;
            page.setBounds(new RectF(0, heightDelta, width * zoom, pageHeight + heightDelta));
        } else {
            final float pageWidth = page.getPageWidth(height, zoom);
            final float widthDelta = (width - pageWidth) / 2;
            page.setBounds(new RectF(widthDelta, 0, pageWidth + widthDelta, height * zoom));
        }
    }

    @Override
    protected boolean isPageVisibleImpl(final Page page) {
        final int pageIndex = page.getIndex();
        if (curler != null) {
            return pageIndex == curler.getForeIndex() || pageIndex == curler.getBackIndex();
        }
        return pageIndex == getCurrentPage();
    }

    @Override
    public void updateAnimationType() {
        final PageAnimationType type = SettingsManager.getBookSettings().getAnimationType();
        curler = PageAnimationType.create(type, this);

        if (curler != null) {
            curler.init();
        }
    }

}
