package org.ebookdroid.core;

import android.graphics.Canvas;
import android.graphics.RectF;

public class ContiniousDocumentView extends AbstractDocumentView {
    public ContiniousDocumentView(IViewerActivity base) {
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
        return getBase().getDocumentModel().getFirstVisiblePage();
    }

    @Override
    protected void onScrollChanged() {
        post(new Runnable() {
            public void run() {
                getBase().getDocumentModel().setCurrentPageByFirstVisible();
            }
        });
        super.onScrollChanged();
    }

    @Override
    protected void verticalConfigScroll(int direction) {
        int scrollheight = getBase().getAppSettings().getScrollHeight();
        getScroller().startScroll(getScrollX(), getScrollY(), 0, (int) (direction * getHeight() * (scrollheight / 100.0)));

        invalidate();
    }

    @Override
    protected void verticalDpadScroll(int direction) {
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
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Page page : getBase().getDocumentModel().getPages().values()) {
            page.draw(canvas);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        invalidatePageSizes();
        invalidateScroll();
        commitZoom();
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

        int width = getWidth();
        float zoom = getBase().getZoomModel().getZoom();

        for (int i = 0; i < getBase().getDocumentModel().getPages().size(); i++) {
            Page page = getBase().getDocumentModel().getPages().get(i);

            float pageHeight = page.getPageHeight(width, zoom);
            page.setBounds(new RectF(0, heightAccum, width * zoom, heightAccum + pageHeight));
            heightAccum += pageHeight;
        }
    }

    @Override
    public boolean isPageTreeNodeVisible(PageTreeNode pageTreeNode) {
        return RectF.intersects(getViewRect(), pageTreeNode.getTargetRectF());
    }

    @Override
    public boolean isPageVisible(Page page) {
        return RectF.intersects(getViewRect(), page.getBounds());
    }

	@Override
	public void updateUseAnimation() {
		// This mode do not use animation

	}
}
