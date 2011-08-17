package org.ebookdroid.core.models;

import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.IViewerActivity;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.PageType;
import org.ebookdroid.core.codec.CodecPageInfo;

import android.view.View;

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
        return pages.get(getCurrentViewPageIndex());
    }

    /**
     * Gets the next page object.
     *
     * @return the next page object
     */
    public Page getNextPageObject() {
        return pages.get(getCurrentViewPageIndex() + 1);
    }

    /**
     * Gets the prev page object.
     *
     * @return the prev page object
     */
    public Page getPrevPageObject() {
        return pages.get(getCurrentViewPageIndex() - 1);
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
        return result;
    }

    public int getLastVisiblePage() {
        int result = 0;
        boolean foundVisible = false;
        for (final Page page : pages.values()) {
            if (page.isVisible()) {
                foundVisible = true;
                result = page.getIndex();
            } else {
                if (foundVisible) {
                    break;
                }
            }
        }
        return result;
    }

    public void setCurrentPageByFirstVisible() {
        int index = getFirstVisiblePage();
        Page page = pages.get(index);
        setCurrentPageIndex(page.getDocumentPageIndex(), page.getIndex());
    }

    public void initPages(final IViewerActivity base) {
        pages.clear();

        final boolean splitPages = base.getBookSettings().getSplitPages();
        final View view = base.getView();

        final CodecPageInfo defCpi = new CodecPageInfo();
        defCpi.setWidth(view.getWidth());
        defCpi.setHeight(view.getHeight());

        int index = 0;

        for (int i = 0; i < getDecodeService().getPageCount(); i++) {
            final CodecPageInfo cpi = getDecodeService().getPageInfo(i);
            if (!splitPages || cpi == null || (cpi.getWidth() < cpi.getHeight())) {
                final Page page = new Page(base, index, i, PageType.FULL_PAGE, cpi != null ? cpi : defCpi);
                pages.put(index++, page);
            } else {
                final Page page1 = new Page(base, index, i, PageType.LEFT_PAGE, cpi);
                pages.put(index++, page1);
                final Page page2 = new Page(base, index, i, PageType.RIGHT_PAGE, cpi);
                pages.put(index++, page2);
            }
        }
    }
}
