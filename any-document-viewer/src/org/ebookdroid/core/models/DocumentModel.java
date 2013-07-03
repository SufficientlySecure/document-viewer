package org.ebookdroid.core.models;

import org.ebookdroid.CodecType;
import org.ebookdroid.R;
import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.ByteBufferManager;
import org.ebookdroid.common.bitmaps.GLBitmaps;
import org.ebookdroid.common.bitmaps.IBitmapRef;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.cache.DocumentCacheFile;
import org.ebookdroid.common.cache.DocumentCacheFile.DocumentInfo;
import org.ebookdroid.common.cache.DocumentCacheFile.PageInfo;
import org.ebookdroid.common.cache.PageCacheFile;
import org.ebookdroid.common.cache.ThumbnailFile;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.types.PageType;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.DecodeServiceBase;
import org.ebookdroid.core.DecodeServiceStub;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecFeatures;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.events.CurrentPageListener;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.IView;

import android.graphics.PointF;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.ui.progress.IProgressIndicator;
import org.emdev.utils.CompareUtils;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.collections.TLIterator;
import org.emdev.utils.listeners.ListenerProxy;

public class DocumentModel extends ListenerProxy {

    protected static final LogContext LCTX = LogManager.root().lctx("DocModel", false);

    private final ThreadLocal<PageIterator> iterators = new ThreadLocal<DocumentModel.PageIterator>();

    public final DecodeService decodeService;

    protected PageIndex currentIndex = PageIndex.FIRST;

    private static final Page[] EMPTY_PAGES = {};

    private final CodecContext context;

    private Page[] pages = EMPTY_PAGES;

    private DocumentCacheFile cacheFile;

    public DocumentInfo docInfo;

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

    public void open(final String fileName, final String password) {
        decodeService.open(fileName, password);
    }

    public Page[] getPages() {
        return pages;
    }

    public PageIterator getPages(final int start) {
        return getPages(start, pages.length);
    }

    public PageIterator getPages(final int start, final int end) {
        final PageIterator pi = iterators.get();
        if (pi == null) {
            return new PageIterator(start, Math.min(end, pages.length));
        }

        pi.index = start;
        pi.end = Math.min(end, pages.length);
        iterators.set(null);
        return pi;
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
            saveDocumentInfo();

            final List<GLBitmaps> bitmapsToRecycle = new ArrayList<GLBitmaps>();
            for (final Page page : pages) {
                page.recycle(bitmapsToRecycle);
            }
            ByteBufferManager.release(bitmapsToRecycle);
            ByteBufferManager.release();
        }
        pages = EMPTY_PAGES;
    }

    public void saveDocumentInfo() {
        final boolean cacheable = decodeService.isFeatureSupported(CodecFeatures.FEATURE_CACHABLE_PAGE_INFO);
        if (cacheable) {
            cacheFile.save(docInfo);
        }
    }

    public Page getPageObject(final int viewIndex) {
        return pages != null && 0 <= viewIndex && viewIndex < pages.length ? pages[viewIndex] : null;
    }

    public Page getPageByDocIndex(final int docIndex) {
        for (final Page page : pages) {
            if (page.index.docIndex == docIndex) {
                return page;
            }
        }
        return null;
    }

    public Page getLinkTargetPage(final int pageDocIndex, final RectF targetRect, final PointF linkPoint,
            final boolean splitRTL) {
        Page target = getPageByDocIndex(pageDocIndex);
        if (target != null) {
            float offsetX = 0;
            float offsetY = 0;
            if (targetRect != null) {
                offsetX = targetRect.left;
                offsetY = targetRect.top;
                if (target.type == PageType.LEFT_PAGE && offsetX >= 0.5f) {
                    target = getPageObject(target.index.viewIndex + (splitRTL ? -1 : +1));
                    offsetX -= 0.5f;
                }
            }
            if (linkPoint != null) {
                linkPoint.set(offsetX, offsetY);
            }
        }
        return target;
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

    public void initPages(final IActivityController base, final IProgressIndicator task) {
        recyclePages();

        final BookSettings bs = base.getBookSettings();

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

            if (docInfo == null) {
                retrieveDocumentInfo(base, bs, task);
            }

            for (int docIndex = 0; docIndex < docInfo.docPageCount; docIndex++) {
                final PageInfo pi = docInfo.docPages.get(docIndex, null);
                final CodecPageInfo info = pi != null ? pi.info : null;
                if (!bs.splitPages || info == null || (info.width < info.height)) {
                    viewIndex = createFullPage(base, docIndex, viewIndex, defCpi, info, pi, list);
                } else {
                    if (bs.splitRTL) {
                        viewIndex = createRightPage(base, docIndex, viewIndex, info, list);
                        viewIndex = createLeftPage(base, docIndex, viewIndex, info, list);
                    } else {
                        viewIndex = createLeftPage(base, docIndex, viewIndex, info, list);
                        viewIndex = createRightPage(base, docIndex, viewIndex, info, list);
                    }
                }
            }
            pages = list.toArray(new Page[list.size()]);
            if (pages.length > 0) {
                createBookThumbnail(bs, pages[0], false, true);
            }
        } finally {
            LCTX.d("Loading page info: " + (System.currentTimeMillis() - start) + " ms");
        }
    }

    protected int createFullPage(final IActivityController base, final int docIndex, final int viewIndex,
            final CodecPageInfo defCpi, final CodecPageInfo info, final PageInfo pi, final ArrayList<Page> list) {
        final PageIndex index = new PageIndex(docIndex, viewIndex);
        final Page page = new Page(base, index, PageType.FULL_PAGE, info != null ? info : defCpi);
        list.add(page);
        page.nodes.root.setInitialCropping(pi);
        return index.viewIndex + 1;
    }

    protected int createLeftPage(final IActivityController base, final int docIndex, final int viewIndex,
            final CodecPageInfo info, final ArrayList<Page> list) {
        final PageIndex index = new PageIndex(docIndex, viewIndex);
        final Page left = new Page(base, index, PageType.LEFT_PAGE, info);
        left.nodes.root.setInitialCropping(docInfo.leftPages.get(docIndex, null));
        list.add(left);
        return index.viewIndex + 1;
    }

    protected int createRightPage(final IActivityController base, final int docIndex, final int viewIndex,
            final CodecPageInfo info, final ArrayList<Page> list) {
        final PageIndex index = new PageIndex(docIndex, viewIndex);
        final Page right = new Page(base, index, PageType.RIGHT_PAGE, info);
        right.nodes.root.setInitialCropping(docInfo.rightPages.get(docIndex, null));
        list.add(right);
        return index.viewIndex + 1;
    }

    public void createBookThumbnail(final BookSettings bs, final Page page, final boolean override,
            final boolean useEmbeddedIfAvailable) {
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

        final IBitmapRef image = decodeService.createThumbnail(useEmbeddedIfAvailable, width, height,
                page.index.docIndex, page.type.getInitialRect());
        thumbnailFile.setImage(image != null ? image.getBitmap() : null);
        BitmapManager.release(image);
    }

    public void updateAutoCropping(final Page page, final RectF r) {
        final PageInfo pageInfo = docInfo.getPageInfo(page);
        pageInfo.autoCropping = r != null ? new RectF(r) : null;
    }

    public void updateManualCropping(final Page page, final RectF r) {
        final PageInfo pageInfo = docInfo.getPageInfo(page);
        pageInfo.manualCropping = r != null ? new RectF(r) : null;
    }

    private DocumentInfo retrieveDocumentInfo(final IActivityController base, final BookSettings bs,
            final IProgressIndicator task) {

        final boolean cacheable = decodeService.isFeatureSupported(CodecFeatures.FEATURE_CACHABLE_PAGE_INFO);

        if (cacheable) {
            cacheFile = CacheManager.getDocumentFile(bs.fileName);
            docInfo = cacheFile.exists() ? cacheFile.load() : null;
            if (docInfo == null) {
                final PageCacheFile pagesFile = CacheManager.getPageFile(bs.fileName);
                docInfo = pagesFile.exists() ? pagesFile.load() : null;
            }
            if (docInfo != null) {
                return docInfo;
            }
        }

        LCTX.d("Retrieving pages from document...");
        docInfo = new DocumentInfo();
        docInfo.docPageCount = decodeService.getPageCount();
        docInfo.viewPageCount = -1;

        final CodecPageInfo unified = decodeService.getUnifiedPageInfo();
        for (int i = 0; i < docInfo.docPageCount; i++) {
            if (task != null) {
                task.setProgressDialogMessage(R.string.msg_getting_page_size, (i + 1), docInfo.docPageCount);
            }
            final PageInfo pi = new PageInfo(i);
            docInfo.docPages.append(i, pi);
            pi.info = unified != null ? unified : decodeService.getPageInfo(i);
        }

        if (cacheable) {
            cacheFile.save(docInfo);
        }
        return docInfo;
    }

    public final class PageIterator implements TLIterator<Page> {

        private int end;
        private int index;

        private PageIterator(final int start, final int end) {
            this.index = start;
            this.end = end;
        }

        @Override
        public boolean hasNext() {
            return 0 <= index && index < end && index < pages.length;
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

        @Override
        public void release() {
            iterators.set(this);
        }
    }
}
