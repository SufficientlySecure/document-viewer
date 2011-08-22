package org.ebookdroid.core;

import android.graphics.RectF;
import android.view.View;

import java.util.Comparator;

public interface IDocumentViewController extends Comparator<PageTreeNode>{

    /* Page related methods */
    void goToPage(int page);

    void invalidatePageSizes();

    void updatePageVisibility();

    boolean isPageVisible(Page page);

    RectF getViewRect();

    int getFirstVisiblePage();

    int getLastVisiblePage();

    void showDocument();

    void redrawView();

    void setAlign(PageAlign byResValue);

    /* Infrastructure methods */

    IViewerActivity getBase();

    View getView();

    void updateAnimationType();
}
