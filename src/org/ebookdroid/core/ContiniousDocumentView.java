package org.ebookdroid.core;

import org.ebookdroid.core.utils.AndroidVersion;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.View;

public class ContiniousDocumentView extends AbstractDocumentView {

    public ContiniousDocumentView(final IViewerActivity base) {
        super(base);
        if (!AndroidVersion.lessThan3x) {
            this.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
    }

    @Override
    protected void goToPageImpl(final int toPage) {
        if (toPage >= 0 && toPage <= getBase().getDocumentModel().getPageCount()) {
            scrollTo(0, getBase().getDocumentModel().getPageObject(toPage).getTop());
        }
    }

    @Override
    public int getCurrentPage() {
        return getBase().getDocumentModel().getFirstVisiblePage();
    }

    @Override
    protected void onScrollChanged() {
        super.onScrollChanged();
        getBase().getDocumentModel().setCurrentPageByFirstVisible();
        redrawView();
    }

    @Override
    protected void verticalConfigScroll(final int direction) {
        final int scrollheight = getBase().getAppSettings().getScrollHeight();
        getScroller().startScroll(getScrollX(), getScrollY(), 0,
                (int) (direction * getHeight() * (scrollheight / 100.0)));

        redrawView();
    }

    @Override
    protected void verticalDpadScroll(final int direction) {
        getScroller().startScroll(getScrollX(), getScrollY(), 0, direction * getHeight() / 2);

        redrawView();
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
    public void drawView(final Canvas canvas, RectF viewRect) {
        for (final Page page : getBase().getDocumentModel().getPages().values()) {
            page.draw(canvas, viewRect);
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

        for (int i = 0; i < getBase().getDocumentModel().getPages().size(); i++) {
            final Page page = getBase().getDocumentModel().getPages().get(i);

            final float pageHeight = page.getPageHeight(width, zoom);
            page.setBounds(new RectF(0, heightAccum, width * zoom, heightAccum + pageHeight));
            heightAccum += pageHeight;
        }
    }

    @Override
    public boolean isPageVisible(final Page page) {
        return RectF.intersects(getViewRect(), page.getBounds());
    }

    @Override
    public void updateAnimationType() {
        // This mode do not use animation

    }
}
