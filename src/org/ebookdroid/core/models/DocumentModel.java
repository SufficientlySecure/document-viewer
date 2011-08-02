package org.ebookdroid.core.models;

import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.IViewerActivity;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.Page.PageType;
import org.ebookdroid.core.codec.CodecPageInfo;

import android.util.Log;

import java.util.Map;
import java.util.TreeMap;

public class DocumentModel extends CurrentPageModel {

    private DecodeService decodeService;

    private final Map<Integer, Page> pages = new TreeMap<Integer, Page>();

    public DocumentModel(final DecodeService decodeService) {
        super();
        this.decodeService = decodeService;
    }

    public Map<Integer, Page> getPages() {
        return pages;
    }

    public int getPageCount() {
        return getPages().size();
    }

    public DecodeService getDecodeService() {
        return decodeService;
    }

    public void recycle() {
        decodeService.recycle();
        decodeService = null;

        pages.clear();
    }

    public Page getPageObject(final int page) {
        return pages.get(page);
    }

    /**
     * Gets the current page object.
     * 
     * @return the current page object
     */
    public Page getCurrentPageObject() {
        return pages.get(getCurrentPageIndex());
    }

    /**
     * Gets the next page object.
     * 
     * @return the next page object
     */
    public Page getNextPageObject() {
        return pages.get(getCurrentPageIndex() + 1);
    }

    /**
     * Gets the prev page object.
     * 
     * @return the prev page object
     */
    public Page getPrevPageObject() {
        return pages.get(getCurrentPageIndex() - 1);
    }

    /**
     * Gets the last page object.
     * 
     * @return the last page object
     */
    public Page getLastPageObject() {
        return pages.get(pages.size() - 1);
    }

    public int getFirstVisiblePage() {
        int result = 0;
        for (final Page page : pages.values()) {
            if (page.isVisible()) {
                result = page.getIndex();
                break;
            }
        }
        Log.d("DocModel", "First visible page: " + result);
        return result;
    }

    public void setCurrentPageByFirstVisible() {
        setCurrentPageIndex(getFirstVisiblePage());
    }

    public void initPages(final IViewerActivity base) {
        pages.clear();
        final boolean splitPages = base.getAppSettings().getSplitPages();

        final int width = base.getView().getWidth();
        final int height = base.getView().getHeight();

        int index = 0;

        for (int i = 0; i < getDecodeService().getPageCount(); i++) {
            final CodecPageInfo cpi = getDecodeService().getPageInfo(i);
            if (!splitPages || cpi == null || (cpi.getWidth() < cpi.getHeight())) {
                final Page page = new Page(base, index, i, PageType.FULL_PAGE);
                page.setAspectRatio(width, height);
                pages.put(index++, page);
            } else {
                final Page page1 = new Page(base, index, i, PageType.LEFT_PAGE);
                page1.setAspectRatio(width, height);
                pages.put(index++, page1);
                final Page page2 = new Page(base, index, i, PageType.RIGHT_PAGE);
                page2.setAspectRatio(width, height);
                pages.put(index++, page2);
            }
        }
    }
}
