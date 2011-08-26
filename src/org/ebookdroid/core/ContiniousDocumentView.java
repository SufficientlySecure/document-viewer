package org.ebookdroid.core;

import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.utils.CompareUtils;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

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
                dm.setCurrentPageIndex(page.getDocumentPageIndex(), page.getIndex());
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
                    dm.setCurrentPageIndex(page.getDocumentPageIndex(), page.getIndex());
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
                        result = page.getIndex();
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
                        result = page.getIndex();
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
        final RectF viewRect = getViewRect();
        final RectF rect1 = node1.getTargetRect(viewRect, node1.page.getBounds());
        final RectF rect2 = node2.getTargetRect(viewRect, node2.page.getBounds());

        final int cp = getCurrentPage();

        final View view = node1.page.base.getView();
        final RectF realViewRect = new RectF(0, 0, view.getWidth(), view.getHeight());

        if (node1.page.index == cp && node2.page.index == cp) {
            int res = CompareUtils.compare(rect1.top, rect2.top);
            if (res == 0) {
                res = CompareUtils.compare(rect1.left, rect2.left);
            }
            return res;
        }

        if (node1.page.index == cp && node2.page.index != cp) {
            return -1;
        }

        if (node1.page.index != cp && node2.page.index == cp) {
            return 1;
        }

        final long centerX = ((long) realViewRect.left + (long) realViewRect.right) / 2;
        final long centerY = ((long) realViewRect.top + (long) realViewRect.bottom) / 2;

        final long centerX1 = ((long) rect1.left + (long) rect1.right) / 2;
        final long centerY1 = ((long) rect1.top + (long) rect1.bottom) / 2;

        final long centerX2 = ((long) rect2.left + (long) rect2.right) / 2;
        final long centerY2 = ((long) rect2.top + (long) rect2.bottom) / 2;

        final long dist1 = (centerX1 - centerX) * (centerX1 - centerX) + (centerY1 - centerY) * (centerY1 - centerY);
        final long dist2 = (centerX2 - centerX) * (centerX2 - centerX) + (centerY2 - centerY) * (centerY2 - centerY);

        int res = CompareUtils.compare(dist1, dist2);
        if (res == 0) {
            res = CompareUtils.compare(rect1.left, rect2.left);
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
            for (final Page page : getBase().getDocumentModel().getPages(changedPage.index)) {
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
