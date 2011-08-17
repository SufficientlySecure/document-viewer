package org.ebookdroid.core;

import android.view.View;

public interface IDocumentViewController {

    /* Page related methods */
    void goToPage(int page);

    void invalidatePageSizes();

    void updatePageVisibility();

    boolean isPageVisible(Page page);

    int getFirstVisiblePage();

    int getLastVisiblePage();

    void showDocument();

    void setAlign(PageAlign byResValue);

    /* Infrastructure methods */

    IViewerActivity getBase();

    View getView();

    void updateAnimationType();
}
