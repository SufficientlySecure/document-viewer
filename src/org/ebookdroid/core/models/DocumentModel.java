package org.ebookdroid.core.models;

import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.IViewerActivity;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.PageType;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.utils.LengthUtils;

import android.view.View;

import java.util.ArrayList;

public class DocumentModel extends CurrentPageModel {

    private static final Page[] EMPTY_PAGES = {};

    private DecodeService decodeService;

    private Page[] pages = EMPTY_PAGES;

    public DocumentModel(final DecodeService decodeService) {
        this.decodeService = decodeService;
    }

    public Page[] getPages() {
        return pages;
    }

    public int getPageCount() {
        return LengthUtils.length(pages);
    }

    public DecodeService getDecodeService() {
        return decodeService;
    }

    public void recycle() {
        decodeService.recycle();
        decodeService = null;
        pages = null;
    }

    public Page getPageObject(final int viewIndex) {
        return pages != null && 0 <= viewIndex && viewIndex < pages.length ? pages[viewIndex] : null;
    }

    /**
     * Gets the current page object.
     *
     * @return the current page object
     */
    public Page getCurrentPageObject() {
        return getPageObject(this.currentViewPageIndex);
    }

    /**
     * Gets the next page object.
     *
     * @return the next page object
     */
    public Page getNextPageObject() {
        return getPageObject(this.currentViewPageIndex + 1);
    }

    /**
     * Gets the prev page object.
     *
     * @return the prev page object
     */
    public Page getPrevPageObject() {
        return getPageObject(this.currentViewPageIndex - 1);
    }

    /**
     * Gets the last page object.
     *
     * @return the last page object
     */
    public Page getLastPageObject() {
        return getPageObject(pages.length - 1);
    }

    public void setCurrentPageByFirstVisible(int firstVisiblePage) {
        Page page = getPageObject(firstVisiblePage);
        if (page != null) {
            setCurrentPageIndex(page.getDocumentPageIndex(), page.getIndex());
        }
    }

    public void initPages(final IViewerActivity base) {
        pages = EMPTY_PAGES;

        final boolean splitPages = base.getBookSettings().getSplitPages();
        final View view = base.getView();

        final CodecPageInfo defCpi = new CodecPageInfo();
        defCpi.setWidth(view.getWidth());
        defCpi.setHeight(view.getHeight());

        int index = 0;

        ArrayList<Page> list = new ArrayList<Page>();
        for (int i = 0; i < getDecodeService().getPageCount(); i++) {
            final CodecPageInfo cpi = getDecodeService().getPageInfo(i);
            if (!splitPages || cpi == null || (cpi.getWidth() < cpi.getHeight())) {
                final Page page = new Page(base, index++, i, PageType.FULL_PAGE, cpi != null ? cpi : defCpi);
                list.add(page);
            } else {
                final Page page1 = new Page(base, index++, i, PageType.LEFT_PAGE, cpi);
                list.add(page1);
                final Page page2 = new Page(base, index++, i, PageType.RIGHT_PAGE, cpi);
                list.add(page2);
            }
        }

        pages = list.toArray(new Page[list.size()]);
    }
}
