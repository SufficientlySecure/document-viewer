package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.core.hwa.IHardwareAcceleration;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.settings.SettingsManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

public class ContiniousDocumentView extends AbstractDocumentView {

    protected static Bitmap dragBitmap;

    public ContiniousDocumentView(final IViewerActivity base) {
        super(base);
        if (dragBitmap == null) {
            dragBitmap = BitmapFactory.decodeResource(base.getContext().getResources(), R.drawable.drag);
        }
        IHardwareAcceleration.Factory.getInstance().setMode(getView().getView(), true);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.AbstractDocumentView#goToPageImpl(int)
     */
    @Override
    protected final void goToPageImpl(final int toPage) {
        final DocumentModel dm = getBase().getDocumentModel();
        final int pageCount = dm.getPageCount();
        if (toPage >= 0 && toPage < pageCount) {
            final Page page = dm.getPageObject(toPage);
            if (page != null) {
                final RectF viewRect = view.getViewRect();
                final RectF bounds = page.getBounds(getBase().getZoomModel().getZoom());
                dm.setCurrentPageIndex(page.index);
                view.scrollTo(getScrollX(), Math.round(bounds.top - (viewRect.height() - bounds.height()) / 2));
            } else {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("No page found for index: " + toPage);
                }
            }
        } else {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Bad page index: " + toPage + ", page count: " + pageCount);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#calculateCurrentPage(org.ebookdroid.core.ViewState)
     */
    @Override
    public final int calculateCurrentPage(final ViewState viewState) {
        int result = 0;
        long bestDistance = Long.MAX_VALUE;

        final int viewY = Math.round(viewState.viewRect.centerY());

        if (viewState.firstVisible != -1) {
            for (final Page page : getBase().getDocumentModel().getPages(viewState.firstVisible,
                    viewState.lastVisible + 1)) {
                final RectF bounds = viewState.getBounds(page);
                final int pageY = Math.round(bounds.centerY());
                final long dist = Math.abs(pageY - viewY);
                if (dist < bestDistance) {
                    bestDistance = dist;
                    result = page.index.viewIndex;
                }
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#verticalConfigScroll(int)
     */
    @Override
    public final void verticalConfigScroll(final int direction) {
        final int scrollheight = SettingsManager.getAppSettings().getScrollHeight();
        final int dy = (int) (direction * getHeight() * (scrollheight / 100.0));

        view.startPageScroll(0, dy);
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
        final Page lpo = getBase().getDocumentModel().getLastPageObject();
        final float zoom = getBase().getZoomModel().getZoom();

        final int bottom = lpo != null ? (int) lpo.getBounds(zoom).bottom - height : 0;
        final int right = (int) (width * zoom) - width;

        return new Rect(0, 0, right, bottom);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.AbstractDocumentView#drawView(android.graphics.Canvas, org.ebookdroid.core.ViewState)
     */
    @Override
    public synchronized final void drawView(final Canvas canvas, final ViewState viewState) {
        final DocumentModel dm = getBase().getDocumentModel();
        if (dm == null) {
            return;
        }
        for (int i = viewState.firstVisible; i <= viewState.lastVisible; i++) {
            final Page page = dm.getPageObject(i);
            if (page != null) {
                page.draw(canvas, viewState);
            }
        }
        if (SettingsManager.getAppSettings().getShowAnimIcon()) {
            DragMark.draw(canvas, viewState);
        }
        view.continueScroll();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.AbstractDocumentView#onLayoutChanged(boolean, boolean, android.graphics.Rect,
     *      android.graphics.Rect)
     */
    @Override
    public final boolean onLayoutChanged(final boolean layoutChanged, final boolean layoutLocked, final Rect oldLaout,
            final Rect newLayout) {
        int page = -1;
        final DocumentModel dm = base.getDocumentModel();
        if (dm == null) {
            return false;
        }

        if (isShown && layoutChanged) {
            page = dm.getCurrentViewPageIndex();
        }
        if (super.onLayoutChanged(layoutChanged, layoutLocked, oldLaout, newLayout)) {
            if (page > 0) {
                goToPage(page);
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#invalidatePageSizes(org.ebookdroid.core.IDocumentViewController.InvalidateSizeReason,
     *      org.ebookdroid.core.Page)
     */
    @Override
    public synchronized final void invalidatePageSizes(final InvalidateSizeReason reason, final Page changedPage) {
        if (!isInitialized) {
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
                heightAccum += pageHeight + 1;
            }
        } else {
            float heightAccum = changedPage.getBounds(1.0f).top;
            for (final Page page : getBase().getDocumentModel().getPages(changedPage.index.viewIndex)) {
                final float pageHeight = width / page.getAspectRatio();
                page.setBounds(new RectF(0, heightAccum, width, heightAccum + pageHeight));
                heightAccum += pageHeight + 1;
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.AbstractDocumentView#isPageVisibleImpl(org.ebookdroid.core.Page, org.ebookdroid.core.ViewState)
     */
    @Override
    protected final boolean isPageVisibleImpl(final Page page, final ViewState viewState) {
        return RectF.intersects(viewState.viewRect, viewState.getBounds(page));
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#updateAnimationType()
     */
    @Override
    public final void updateAnimationType() {
        // This mode do not use animation
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.IDocumentViewController#pageUpdated(int)
     */
    @Override
    public void pageUpdated(final int viewIndex) {
    }
}
