package org.ebookdroid.core;

import android.graphics.RectF;
import android.view.View;

public interface IDocumentViewController {

    /* Page related methods */
    void goToPage(int page);

    void invalidatePageSizes();

    RectF getViewRect();

    void updatePageVisibility();

    boolean isPageVisible(Page page);

    boolean shouldKeptInMemory(PageTreeNode pageTreeNode);

    void showDocument();

    void setAlign(PageAlign byResValue);

    /* Infrastructure methods */

    IViewerActivity getBase();

    View getView();

    void updateAnimationType();
}
