package org.ebookdroid.core;

import android.graphics.RectF;
import android.view.View;

public interface IDocumentViewController {

    /* Page related methods */
    void goToPage(int page);

    void invalidatePageSizes(InvalidateSizeReason reason, Page changedPage);

    RectF getViewRect();

    int getFirstVisiblePage();

    int calculateCurrentPage(ViewState viewState);

    int getLastVisiblePage();

    void verticalDpadScroll(int i);

    void verticalConfigScroll(int i);

    void redrawView();

    void redrawView(ViewState viewState);

    void changeLayoutLock(boolean lock);

    void setAlign(PageAlign byResValue);

    /* Infrastructure methods */

    IViewerActivity getBase();

    View getView();

    void updateAnimationType();

    void updateMemorySettings();

    public static enum InvalidateSizeReason {
        INIT, LAYOUT, PAGE_ALIGN, ZOOM, PAGE_LOADED;
    }

}
