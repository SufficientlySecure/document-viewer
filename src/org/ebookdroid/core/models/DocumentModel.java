package org.ebookdroid.core.models;

import org.ebookdroid.CodecType;
import org.ebookdroid.R;
import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.common.bitmaps.Bitmaps;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.cache.PageCacheFile;
import org.ebookdroid.common.cache.ThumbnailFile;
import org.ebookdroid.common.log.LogContext;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.types.PageType;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.DecodeServiceBase;
import org.ebookdroid.core.DecodeServiceStub;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.events.CurrentPageListener;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.IView;

import android.graphics.RectF;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.emdev.utils.CompareUtils;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.listeners.ListenerProxy;

public class DocumentModel extends ListenerProxy {

    protected static final LogContext LCTX = LogContext.ROOT.lctx("DocModel");

    protected PageIndex currentIndex = PageIndex.FIRST;

    private static final Page[] EMPTY_PAGES = {};

    private final CodecContext context;
    private final DecodeService decodeService;

    private Page[] pages = EMPTY_PAGES;

    public DocumentModel(final CodecType activityType) {
        super(CurrentPageListener.class);
        if (activityType != null) {
            try {
                context = activityType.getContextClass().newInstance();
                decodeService = new DecodeServiceBase(context);
            } catch (final Throwable th) {
                throw new RuntimeException(th);
            }
        } else {
            context = null;
            decodeService = new DecodeServiceStub();
        }
    }

    public void open(String fileName, String password) {
        decodeService.open(fileName, password);
    }

    public DecodeService getDecodeService() {
        return decodeService;
    }

    public Page[] getPages() {
        return pages;
    }

    public Iterable<Page> getPages(final int start) {
        return new PageIterator(start, pages.length);
    }

    public Iterable<Page> getPages(final int start, final int end) {
        return new PageIterator(start, Math.min(end, pages.length));
    }

    public int getPageCount() {
        return LengthUtils.length(pages);
    }

    public void recycle() {
        decodeService.recycle();
        recyclePages();
    }

    private void recyclePages() {
        if (LengthUtils.isNotEmpty(pages)) {
            final List<Bitmaps> bitmapsToRecycle = new ArrayList<Bitmaps>();
            for (final Page page : pages) {
                page.recycle(bitmapsToRecycle);
            }
            BitmapManager.release(bitmapsToRecycle);
            BitmapManager.release();
        }
        pages = EMPTY_PAGES;
    }

    public Page getPageObject(final int viewIndex) {
        return pages != null && 0 <= viewIndex && viewIndex < pages.length ? pages[viewIndex] : null;
    }

    public Page getPageByDocIndex(final int docIndex) {
        for(Page page  : pages) {
            if (page.index.docIndex == docIndex) {
                return page;
            }
        }
        return null;
    }

    /**
     * Gets the current page object.
     * 
     * @return the current page object
     */
    public Page getCurrentPageObject() {
        return getPageObject(this.currentIndex.viewIndex);
    }

    /**
     * Gets the last page object.
     * 
     * @return the last page object
     */
    public Page getLastPageObject() {
        return getPageObject(pages.length - 1);
    }

    public void setCurrentPageIndex(final PageIndex newIndex) {
        if (!CompareUtils.equals(currentIndex, newIndex)) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Current page changed: " + "currentIndex" + " -> " + newIndex);
            }

            final PageIndex oldIndex = this.currentIndex;
            this.currentIndex = newIndex;

            this.<CurrentPageListener> getListener().currentPageChanged(oldIndex, newIndex);
        }
    }

    public PageIndex getCurrentIndex() {
        return this.currentIndex;
    }

    public int getCurrentViewPageIndex() {
        return this.currentIndex.viewIndex;
    }

    public int getCurrentDocPageIndex() {
        return this.currentIndex.docIndex;
    }

    public void setCurrentPageByFirstVisible(final int firstVisiblePage) {
        final Page page = getPageObject(firstVisiblePage);
        if (page != null) {
            setCurrentPageIndex(page.index);
        }
    }

    public void initPages(final IActivityController base, final IActivityController.IBookLoadTask task) {
        recyclePages();

        final BookSettings bs = SettingsManager.getBookSettings();

        if (base == null || bs == null || context == null || decodeService == null) {
            return;
        }

        final IView view = base.getView();

        final CodecPageInfo defCpi = new CodecPageInfo();
        defCpi.width = (view.getWidth());
        defCpi.height = (view.getHeight());

        int viewIndex = 0;

        final long start = System.currentTimeMillis();
        try {
            final ArrayList<Page> list = new ArrayList<Page>();
            final CodecPageInfo[] infos = retrievePagesInfo(base, bs, task);

            for (int docIndex = 0; docIndex < infos.length; docIndex++) {
                if (!bs.splitPages || infos[docIndex] == null || (infos[docIndex].width < infos[docIndex].height)) {
                    final Page page = new Page(base, new PageIndex(docIndex, viewIndex++), PageType.FULL_PAGE,
                            infos[docIndex] != null ? infos[docIndex] : defCpi);
                    list.add(page);
                } else {
                    final Page page1 = new Page(base, new PageIndex(docIndex, viewIndex++), PageType.LEFT_PAGE,
                            infos[docIndex]);
                    list.add(page1);
                    final Page page2 = new Page(base, new PageIndex(docIndex, viewIndex++), PageType.RIGHT_PAGE,
                            infos[docIndex]);
                    list.add(page2);
                }
            }
            pages = list.toArray(new Page[list.size()]);
            if (pages.length > 0) {
                createBookThumbnail(bs, pages[0], false);
            }
        } finally {
            LCTX.d("Loading page info: " + (System.currentTimeMillis() - start) + " ms");
        }
    }

    public void createBookThumbnail(final BookSettings bs, final Page page, final boolean override) {
        final ThumbnailFile thumbnailFile = CacheManager.getThumbnailFile(bs.fileName);
        if (!override && thumbnailFile.exists()) {
            return;
        }

        int width = 200, height = 200;
        final RectF bounds = page.getBounds(1.0f);
        final float pageWidth = bounds.width();
        final float pageHeight = bounds.height();

        if (pageHeight > pageWidth) {
            width = (int) (200 * pageWidth / pageHeight);
        } else {
            height = (int) (200 * pageHeight / pageWidth);
        }

        BitmapRef image = decodeService.createThumbnail(width, height, page.index.docIndex, page.type.getInitialRect());
        thumbnailFile.setImage(image != null ? image.getBitmap() : null);
        BitmapManager.release(image);
    }

    private CodecPageInfo[] retrievePagesInfo(final IActivityController base, final BookSettings bs,
            final IActivityController.IBookLoadTask task) {
        final PageCacheFile pagesFile = CacheManager.getPageFile(bs.fileName);

        if (decodeService.isPageSizeCacheable() && pagesFile.exists()) {
            final CodecPageInfo[] infos = pagesFile.load();
            if (infos != null) {
                return infos;
            }
        }

        final CodecPageInfo[] infos = new CodecPageInfo[getDecodeService().getPageCount()];
        final CodecPageInfo unified = decodeService.getUnifiedPageInfo();
        for (int i = 0; i < infos.length; i++) {
            if (task != null) {
                task.setProgressDialogMessage(R.string.msg_getting_page_size, (i + 1), infos.length);
            }
            infos[i] = unified != null ? unified : getDecodeService().getPageInfo(i);
        }

        if (decodeService.isPageSizeCacheable()) {
            pagesFile.save(infos);
        }
        return infos;
    }

    private final class PageIterator implements Iterable<Page>, Iterator<Page> {

        private final int end;
        private int index;

        private PageIterator(final int start, final int end) {
            this.index = start;
            this.end = end;
        }

        @Override
        public boolean hasNext() {
            return 0 <= index && index < end;
        }

        @Override
        public Page next() {
            return hasNext() ? pages[index++] : null;
        }

        @Override
        public void remove() {
        }

        @Override
        public Iterator<Page> iterator() {
            return this;
        }
    }
}
