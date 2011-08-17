package org.ebookdroid.core;

import org.ebookdroid.core.models.DocumentModel;

import android.graphics.Canvas;
import android.graphics.RectF;

public class ContiniousDocumentView extends AbstractDocumentView {

    public ContiniousDocumentView(final IViewerActivity base) {
        super(base);
    }

    @Override
    protected void goToPageImpl(final int toPage) {
        if (toPage >= 0 && toPage <= getBase().getDocumentModel().getPageCount()) {
            scrollTo(0, getBase().getDocumentModel().getPageObject(toPage).getTop());
        }
    }

    @Override
    public int getCurrentPage() {
        return getFirstVisiblePage();
    }

    @Override
    protected void onScrollChanged() {
        post(new Runnable() {

            @Override
            public void run() {
                updatePageVisibility();
                getBase().getDocumentModel().setCurrentPageByFirstVisible(getFirstVisiblePage());
            }
        });
        super.onScrollChanged();
    }

    @Override
    protected void verticalConfigScroll(final int direction) {
        final int scrollheight = getBase().getAppSettings().getScrollHeight();
        getScroller().startScroll(getScrollX(), getScrollY(), 0,
                (int) (direction * getHeight() * (scrollheight / 100.0)));

        invalidate();
    }

    @Override
    protected void verticalDpadScroll(final int direction) {
        getScroller().startScroll(getScrollX(), getScrollY(), 0, direction * getHeight() / 2);

        invalidate();
    }

    @Override
    protected int getTopLimit() {
        return 0;
    }

    @Override
    protected int getLeftLimit() {
        return 0;
    }

    @Override
    protected int getBottomLimit() {
        return (int) getBase().getDocumentModel().getLastPageObject().getBounds().bottom - getHeight();
    }

    @Override
    protected int getRightLimit() {
        return (int) (getWidth() * getBase().getZoomModel().getZoom()) - getWidth();
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        DocumentModel dm = getBase().getDocumentModel();
        for (int i = firstVisiblePage; i <= lastVisiblePage; i++) {
            final Page page = dm.getPageObject(i);
            if (page != null) {
                page.draw(canvas);
            }
        }
    }

    @Override
    protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
        int page = -1;
        if (changed) {
            page = getCurrentPage();
        }
        super.onLayout(changed, left, top, right, bottom);

        invalidatePageSizes();
        invalidateScroll();
        commitZoom();
        if (page > 0) {
            goToPage(page);
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
        float heightAccum = 0;

        final int width = getWidth();
        final float zoom = getBase().getZoomModel().getZoom();

        for (final Page page : getBase().getDocumentModel().getPages()) {
            final float pageHeight = page.getPageHeight(width, zoom);
            page.setBounds(new RectF(0, heightAccum, width * zoom, heightAccum + pageHeight));
            heightAccum += pageHeight;
        }
    }

    @Override
    protected boolean isPageVisibleImpl(final Page page) {
        return RectF.intersects(getViewRect(), page.getBounds());
    }

    @Override
    public void updateAnimationType() {
        // This mode do not use animation

    }
}
