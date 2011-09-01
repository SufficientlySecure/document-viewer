package org.ebookdroid.core;

import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.utils.CompareUtils;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

public class ContiniousDocumentView extends AbstractDocumentView {

    public ContiniousDocumentView(final IViewerActivity base) {
        super(base);
    }

    @Override
    protected void goToPageImpl(final int toPage) {
        final DocumentModel dm = getBase().getDocumentModel();
        if (toPage >= 0 && toPage < dm.getPageCount()) {
            final Page page = dm.getPageObject(toPage);
            if (page != null) {
                final RectF viewRect = this.getViewRect();
                final RectF bounds = page.getBounds();
                dm.setCurrentPageIndex(page.index);
                scrollTo(0, page.getTop() - ((int) viewRect.height() - (int) bounds.height()) / 2);
            }
        }
    }

    public void setCurrentPageByFirstVisible() {
        final DocumentModel dm = getBase().getDocumentModel();
        final int index = getCurrentPage();
        final Page page = dm.getPageObject(index);
        if (page != null) {
            post(new Runnable() {

                @Override
                public void run() {
                    dm.setCurrentPageIndex(page.index);
                }
            });
        }
    }

    @Override
    public int getCurrentPage() {
        int result = 0;
        long bestDistance = Long.MAX_VALUE;

        final RectF viewRect = getViewRect();
        final int viewY = Math.round((viewRect.top + viewRect.bottom) / 2);

        boolean foundVisible = false;
        if (firstVisiblePage != -1) {
            for (final Page page : getBase().getDocumentModel().getPages(firstVisiblePage, lastVisiblePage + 1)) {
                if (page.isVisible()) {
                    foundVisible = true;
                    final RectF bounds = page.getBounds();
                    final int pageY = Math.round((bounds.top + bounds.bottom) / 2);
                    final long dist = Math.abs(pageY - viewY);
                    if (dist < bestDistance) {
                        bestDistance = dist;
                        result = page.index.viewIndex;
                    }
                } else {
                    if (foundVisible) {
                        break;
                    }
                }
            }
        } else {
            for (final Page page : getBase().getDocumentModel().getPages()) {
                if (page.isVisible()) {
                    foundVisible = true;
                    final RectF bounds = page.getBounds();
                    final int pageY = Math.round((bounds.top + bounds.bottom) / 2);
                    final long dist = Math.abs(pageY - viewY);
                    if (dist < bestDistance) {
                        bestDistance = dist;
                        result = page.index.viewIndex;
                    }
                } else {
                    if (foundVisible) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public int compare(final PageTreeNode node1, final PageTreeNode node2) {
        final int cp = getCurrentPage();
        final int viewIndex1 = node1.page.index.viewIndex;
        final int viewIndex2 = node2.page.index.viewIndex;

        int res = 0;

        if (viewIndex1 == cp && viewIndex2 == cp) {
            res = CompareUtils.compare(node1.pageSliceBounds.top, node2.pageSliceBounds.top);
            if (res == 0) {
                res = CompareUtils.compare(node1.pageSliceBounds.left, node2.pageSliceBounds.left);
            }
        } else {
            float d1 = viewIndex1 + node1.pageSliceBounds.top - (cp + 0.5f);
            float d2 = viewIndex2 + node2.pageSliceBounds.top - (cp + 0.5f);
            final int dist1 = Math.abs((int) (d1 * node1.childrenZoomThreshold));
            final int dist2 = Math.abs((int) (d2 * node2.childrenZoomThreshold));
            res = CompareUtils.compare(dist1, dist2);
            if (res == 0) {
                res = -CompareUtils.compare(viewIndex1, viewIndex2);
            }
        }
        return res;
    }

    @Override
    protected void onScrollChanged(final int newPage, final int direction) {
        super.onScrollChanged(newPage, direction);
        setCurrentPageByFirstVisible();
        redrawView();
    }

    @Override
    protected void verticalConfigScroll(final int direction) {
        final int scrollheight = SettingsManager.getAppSettings().getScrollHeight();
        final int dy = (int) (direction * getHeight() * (scrollheight / 100.0));

        getScroller().startScroll(getScrollX(), getScrollY(), 0, dy);
        scrollBy(0, dy);

        redrawView();
    }

    @Override
    protected void verticalDpadScroll(final int direction) {
        final int dy = direction * getHeight() / 2;

        getScroller().startScroll(getScrollX(), getScrollY(), 0, dy);
        scrollBy(0, dy);

        redrawView();
    }

    @Override
    protected Rect getScrollLimits() {
        final int width = getWidth();
        final int height = getHeight();
        final Page lpo = getBase().getDocumentModel().getLastPageObject();

        final int bottom = lpo != null ? (int) lpo.getBounds().bottom - height : 0;
        final int right = (int) (width * getBase().getZoomModel().getZoom()) - width;

        return new Rect(0, 0, right, bottom);
    }

    @Override
    public synchronized void drawView(final Canvas canvas, final RectF viewRect) {
        final DocumentModel dm = getBase().getDocumentModel();
        for (int i = firstVisiblePage; i <= lastVisiblePage; i++) {
            final Page page = dm.getPageObject(i);
            if (page != null) {
                page.draw(canvas, viewRect);
            }
        }
        setCurrentPageByFirstVisible();
    }

    @Override
    protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
        int page = -1;
        if (changed) {
            page = getCurrentPage();
        }
        super.onLayout(changed, left, top, right, bottom);

        invalidatePageSizes(InvalidateSizeReason.LAYOUT, null);
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
    public synchronized void invalidatePageSizes(final InvalidateSizeReason reason, final Page changedPage) {
        if (!isInitialized()) {
            return;
        }

        if (reason == InvalidateSizeReason.PAGE_ALIGN) {
            return;
        }

        if (reason == InvalidateSizeReason.ZOOM) {
            return;
        }

        final int width = getWidth();

        if (changedPage == null) {
            float heightAccum = 0;
            for (final Page page : getBase().getDocumentModel().getPages()) {
                final float pageHeight = width / page.getAspectRatio();
                page.setBounds(new RectF(0, heightAccum, width, heightAccum + pageHeight));
                heightAccum += pageHeight;
            }
        } else {
            float heightAccum = changedPage.getBounds().top;
            for (final Page page : getBase().getDocumentModel().getPages(changedPage.index.viewIndex)) {
                final float pageHeight = width / page.getAspectRatio();
                page.setBounds(new RectF(0, heightAccum, width, heightAccum + pageHeight));
                heightAccum += pageHeight;
            }
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
