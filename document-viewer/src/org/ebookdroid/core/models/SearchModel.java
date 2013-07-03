package org.ebookdroid.core.models;

import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.codec.CodecFeatures;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.IViewController;

import android.graphics.RectF;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.utils.CompareUtils;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.collections.SparseArrayEx;

public class SearchModel {

    protected static final LogContext LCTX = LogManager.root().lctx("SearchModel", true);

    private final IActivityController base;
    private String pattern;
    private Page currentPage;
    private int currentMatchIndex;
    private final SparseArrayEx<Matches> matches;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public SearchModel(final IActivityController base) {
        this.base = base;
        this.matches = new SparseArrayEx<Matches>();
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(final String pattern) {
        final String p = pattern != null ? pattern.toLowerCase() : null;
        if (!CompareUtils.equals(this.pattern, p)) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("SearchModel.setPattern(" + p + ")");
            }
            lock.writeLock().lock();
            try {
                this.pattern = p;
                for (final Matches ref : matches) {
                    final Matches m = ref;
                    if (m != null) {
                        m.cancel();
                    }
                }
                this.matches.clear();
                this.currentPage = null;
                this.currentMatchIndex = -1;
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    public Matches getMatches(final Page page) {
        if (LengthUtils.isEmpty(this.pattern)) {
            return null;
        }
        return getMatches(page.index.docIndex);
    }

    protected Matches getMatches(final int key) {
        lock.readLock().lock();
        try {
            final Matches ref = matches.get(key);
            return ref;
        } finally {
            lock.readLock().unlock();
        }
    }

    protected Matches getOrCreateMatches(final int key) {
        lock.writeLock().lock();
        try {
            Matches ref = matches.get(key);
            Matches m = ref;
            if (m == null) {
                m = new Matches(key);
                ref = m;
                matches.put(key, ref);
            }
            return m;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Page getCurrentPage() {
        return currentPage;
    }

    public int getCurrentMatchIndex() {
        return currentMatchIndex;
    }

    public RectF getCurrentRegion() {
        if (currentPage == null) {
            return null;
        }
        final Matches m = getMatches(currentPage.index.docIndex);
        if (m == null) {
            return null;
        }
        final List<? extends RectF> mm = m.getMatches();
        if (0 <= currentMatchIndex && currentMatchIndex < LengthUtils.length(mm)) {
            return mm.get(currentMatchIndex);
        }
        return null;
    }

    public RectF moveToNext(final ProgressCallback callback) {
        final IViewController ctrl = base.getDocumentController();
        final int firstVisiblePage = ctrl.getFirstVisiblePage();
        final int lastVisiblePage = ctrl.getLastVisiblePage();

        if (currentPage == null) {
            return searchFirstFrom(firstVisiblePage, callback);
        }

        final Matches m = getMatches(currentPage.index.docIndex);
        if (m == null) {
            return searchFirstFrom(currentPage.index.viewIndex, callback);
        }

        if (firstVisiblePage <= currentPage.index.viewIndex && currentPage.index.viewIndex <= lastVisiblePage) {
            currentMatchIndex++;
            final List<? extends RectF> mm = m.getMatches();
            if (0 <= currentMatchIndex && currentMatchIndex < LengthUtils.length(mm)) {
                return mm.get(currentMatchIndex);
            } else {
                return searchFirstFrom(currentPage.index.viewIndex + 1, callback);
            }
        } else {
            return searchFirstFrom(firstVisiblePage, callback);
        }
    }

    public RectF moveToPrev(final ProgressCallback callback) {
        final IViewController ctrl = base.getDocumentController();
        final int firstVisiblePage = ctrl.getFirstVisiblePage();
        final int lastVisiblePage = ctrl.getLastVisiblePage();

        if (currentPage == null) {
            return searchLastFrom(lastVisiblePage, callback);
        }

        final Matches m = getMatches(currentPage.index.docIndex);
        if (m == null) {
            return searchLastFrom(currentPage.index.viewIndex, callback);
        }

        if (firstVisiblePage <= currentPage.index.viewIndex && currentPage.index.viewIndex <= lastVisiblePage) {
            currentMatchIndex--;
            final List<? extends RectF> mm = m.getMatches();
            if (0 <= currentMatchIndex && currentMatchIndex < LengthUtils.length(mm)) {
                return mm.get(currentMatchIndex);
            } else {
                return searchLastFrom(currentPage.index.viewIndex - 1, callback);
            }
        } else {
            return searchLastFrom(lastVisiblePage, callback);
        }
    }

    private RectF searchFirstFrom(final int pageIndex, final ProgressCallback callback) {
        if (LengthUtils.isEmpty(this.pattern)) {
            return null;
        }
        if (LCTX.isDebugEnabled()) {
            LCTX.d("SearchModel.searchFirstFrom(" + pageIndex + "): start");
        }
        final int pageCount = base.getDocumentModel().getPageCount();

        currentPage = null;
        currentMatchIndex = -1;

        int index = pageIndex - 1;
        while (!callback.isCancelled() && ++index < pageCount) {
            final Page p = base.getDocumentModel().getPageObject(index);
            if (callback != null) {
                callback.searchStarted(index);
            }

            if (LCTX.isDebugEnabled()) {
                LCTX.d("SearchModel.searchFirstFrom(" + pageIndex + "): >>> " + index);
            }
            final Matches m = startSearchOnPage(p);
            final List<? extends RectF> mm = m.waitForMatches();
            if (LCTX.isDebugEnabled()) {
                LCTX.d("SearchModel.searchFirstFrom(" + pageIndex + "): <<< " + index);
            }

            if (callback != null) {
                callback.searchFinished(index);
            }
            if (LengthUtils.isNotEmpty(mm)) {
                currentPage = p;
                currentMatchIndex = 0;
                return mm.get(currentMatchIndex);
            }
        }
        if (LCTX.isDebugEnabled()) {
            LCTX.d("SearchModel.searchFirstFrom(" + pageIndex + "): end");
        }
        return null;
    }

    private RectF searchLastFrom(final int pageIndex, final ProgressCallback callback) {
        if (LengthUtils.isEmpty(this.pattern)) {
            return null;
        }
        currentPage = null;
        currentMatchIndex = -1;

        int index = pageIndex + 1;
        while (!callback.isCancelled() && 0 <= --index) {
            final Page p = base.getDocumentModel().getPageObject(index);
            if (callback != null) {
                callback.searchStarted(index);
            }

            final Matches m = startSearchOnPage(p);
            final List<? extends RectF> mm = m.waitForMatches();

            if (callback != null) {
                callback.searchFinished(index);
            }
            if (LengthUtils.isNotEmpty(mm)) {
                currentPage = p;
                currentMatchIndex = mm.size() - 1;
                return mm.get(currentMatchIndex);
            }
        }
        return null;
    }

    private Matches startSearchOnPage(final Page page) {
        final Matches m = getOrCreateMatches(page.index.docIndex);
        m.startSearchOnPage(base.getDecodeService(), page, pattern);
        return m;
    }

    public static class Matches implements DecodeService.SearchCallback {

        static final AtomicLong SEQ = new AtomicLong();
        final long id = SEQ.getAndIncrement();
        final int key;
        final AtomicReference<CountDownLatch> running = new AtomicReference<CountDownLatch>();
        final AtomicReference<List<? extends RectF>> matches = new AtomicReference<List<? extends RectF>>();

        Matches(final int key) {
            this.key = key;
        }

        public void cancel() {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Matches.cancel(" + key + ")");
            }
            setMatches(null);
        }

        public void startSearchOnPage(final DecodeService ds, final Page page, final String pattern) {
            if (running.compareAndSet(null, new CountDownLatch(1))) {
                if (!ds.isFeatureSupported(CodecFeatures.FEATURE_TEXT_SEARCH)) {
                    if (LCTX.isDebugEnabled()) {
                        LCTX.d("Matches.startSearchOnPage(" + id + ", " + key + "): search not supported");
                    }
                    setMatches(null);
                    return;
                }
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("Matches.startSearchOnPage(" + id + ", " + key + ")");
                }
                ds.searchText(page, pattern, this);
            }
        }

        public void setMatches(final List<? extends RectF> matches) {
            this.matches.set(matches);
            final CountDownLatch event = running.getAndSet(null);
            if (event != null) {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("SearchModel.Matches.setMatches(" + id + ", " + key + "): " + matches);
                }
                event.countDown();
            }
        }

        public List<? extends RectF> getMatches() {
            return this.matches.get();
        }

        public List<? extends RectF> waitForMatches() {
            final CountDownLatch event = running.get();
            if (event != null) {
                try {
                    event.await();
                } catch (final InterruptedException ex) {
                    Thread.interrupted();
                }
            }
            final List<? extends RectF> res = this.matches.get();
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Matches.waitForMatches(" + id + ", " + key + "): " + res);
            }
            return res;
        }

        @Override
        public void searchComplete(final Page page, final List<? extends RectF> regions) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Matches.searchComplete(" + id + ", " + key + "): " + regions);
            }
            this.setMatches(regions);
        }
    }

    public static interface ProgressCallback {

        void searchStarted(int pageIndex);

        void searchFinished(int pageIndex);

        boolean isCancelled();
    }
}
