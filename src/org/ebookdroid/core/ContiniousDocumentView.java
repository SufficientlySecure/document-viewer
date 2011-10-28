package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.settings.SettingsManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class ContiniousDocumentView extends AbstractDocumentView {

    protected static Bitmap dragBitmap;

    public ContiniousDocumentView(final IViewerActivity base) {
        super(base);
        if (dragBitmap == null) {
            dragBitmap = BitmapFactory.decodeResource(base.getContext().getResources(), R.drawable.drag);
        }
    }

    @Override
    protected final void goToPageImpl(final int toPage) {
        final DocumentModel dm = getBase().getDocumentModel();
        if (toPage >= 0 && toPage < dm.getPageCount()) {
            final Page page = dm.getPageObject(toPage);
            if (page != null) {
                final RectF viewRect = view.getViewRect();
                final RectF bounds = page.getBounds(getBase().getZoomModel().getZoom());

                dm.setCurrentPageIndex(page.index);
                view.scrollTo(getScrollX(), Math.round(bounds.top - (viewRect.height() - bounds.height()) / 2));
            }
        }
    }

    public final void setCurrentPageByFirstVisible() {
    }

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

    @Override
    public final void onScrollChanged(final int newPage, final int direction) {
        // bounds could be not updated
        if (inZoom.get()) {
            return;
        }

        final Runnable r = new Runnable() {

            @Override
            public void run() {
                final ViewState viewState = updatePageVisibility(newPage, direction, getBase().getZoomModel().getZoom());

                final DocumentModel dm = getBase().getDocumentModel();
                final Page page = dm.getPageObject(viewState.currentIndex);
                if (page != null) {
                    dm.setCurrentPageIndex(page.index);
                    view.redrawView(viewState);
                }
            }
        };

        // on scrollChanged can be called from scrollTo just after new layout applied so we should wait for relayout
        base.getActivity().runOnUiThread(r);
    }

    @Override
    public final void verticalConfigScroll(final int direction) {
        final int scrollheight = SettingsManager.getAppSettings().getScrollHeight();
        final int dy = (int) (direction * getHeight() * (scrollheight / 100.0));

        view.startPageScroll(dy);
    }

    @Override
    public final void verticalDpadScroll(final int direction) {
        final int dy = direction * getHeight() / 2;

        view.startPageScroll(dy);
    }

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

    @Override
    public synchronized final void drawView(final Canvas canvas, final ViewState viewState) {
        final DocumentModel dm = getBase().getDocumentModel();
        for (int i = viewState.firstVisible; i <= viewState.lastVisible; i++) {
            final Page page = dm.getPageObject(i);
            if (page != null) {
                page.draw(canvas, viewState);
            }
        }

        Rect l = getScrollLimits();
        if (l.width() > 0) {
            final Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            paint.setDither(true);
            canvas.drawBitmap(dragBitmap, view.getWidth() - dragBitmap.getWidth(),
                    view.getHeight() - dragBitmap.getHeight(), paint);
        }

        view.continueScroll();
    }

    @Override
    public final boolean onLayoutChanged(final boolean layoutChanged, boolean layoutLocked, final int left,
            final int top, final int right, final int bottom) {
        int page = -1;
        if (isShown && layoutChanged) {
            page = base.getDocumentModel().getCurrentViewPageIndex();
        }
        if (super.onLayoutChanged(layoutChanged, layoutLocked, left, top, right, bottom)) {
            if (page > 0) {
                goToPage(page);
            }
            return true;
        }
        return false;
    }

    /**
     * Invalidate page sizes.
     */
    @Override
    public synchronized final void invalidatePageSizes(final InvalidateSizeReason reason, final Page changedPage) {
        if (!isShown()) {
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
            float heightAccum = changedPage.getBounds(1.0f).top;
            for (final Page page : getBase().getDocumentModel().getPages(changedPage.index.viewIndex)) {
                final float pageHeight = width / page.getAspectRatio();
                page.setBounds(new RectF(0, heightAccum, width, heightAccum + pageHeight));
                heightAccum += pageHeight;
            }
        }
    }

    @Override
    protected final boolean isPageVisibleImpl(final Page page, final ViewState viewState) {
        return RectF.intersects(viewState.viewRect, viewState.getBounds(page));
    }

    @Override
    public final void updateAnimationType() {
        // This mode do not use animation

    }
}
