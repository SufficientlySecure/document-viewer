package org.ebookdroid.core.models;

import org.ebookdroid.common.log.LogContext;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.Page;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.IViewController;

import android.graphics.RectF;
import android.util.SparseArray;

import java.util.List;

import org.emdev.utils.CompareUtils;
import org.emdev.utils.LengthUtils;

public class SearchModel implements DecodeService.SearchCallback {

    protected static final LogContext LCTX = LogContext.ROOT.lctx("SearchModel");

    private final IActivityController base;
    private String pattern;
    private Page currentPage;
    private int currentMatchIndex;
    private final SparseArray<Matches> matches;

    public SearchModel(final IActivityController base) {
        this.base = base;
        this.matches = new SparseArray<Matches>();
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(final String pattern) {
        if (!CompareUtils.equals(this.pattern, pattern)) {
            this.pattern = pattern;
            this.matches.clear();
            this.currentPage = null;
            this.currentMatchIndex = -1;
        }
    }

    public Matches getMatches(final Page page) {
        if (LengthUtils.isEmpty(this.pattern)) {
            return null;
        }
        final int key = page.index.docIndex;
        return matches.get(key);
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
        final Matches m = matches.get(currentPage.index.docIndex);
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

        final Matches m = matches.get(currentPage.index.docIndex);
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

        final Matches m = matches.get(currentPage.index.docIndex);
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
        final int pageCount = base.getDocumentModel().getPageCount();

        currentPage = null;
        currentMatchIndex = -1;

        int index = pageIndex - 1;
        while (!callback.isCancelled() && ++index < pageCount) {
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
                currentMatchIndex = 0;
                return mm.get(currentMatchIndex);
            }
        }
        return null;
    }

    private RectF searchLastFrom(final int pageIndex, final ProgressCallback callback) {
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
        if (LengthUtils.isEmpty(this.pattern)) {
            return null;
        }
        final int key = page.index.docIndex;
        Matches m = matches.get(key);
        if (m == null) {
            m = new Matches();
            matches.put(key, m);
            base.getDecodeService().searchText(page, pattern, this);
        }
        return m;
    }

    @Override
    public void searchComplete(final Page page, final List<? extends RectF> regions) {
        final int key = page.index.docIndex;
        Matches m = matches.get(key);
        if (m == null) {
            m = new Matches();
            matches.put(key, m);
        }
        m.setMatches(regions);
    }

    public static class Matches {

        private List<? extends RectF> matches;

        public synchronized void setMatches(final List<? extends RectF> matches) {
            this.matches = matches;
            this.notify();
        }

        public synchronized List<? extends RectF> getMatches() {
            return this.matches;
        }

        public synchronized List<? extends RectF> waitForMatches() {
            if (this.matches == null) {
                try {
                    this.wait();
                } catch (final InterruptedException ex) {
                    Thread.interrupted();
                }

            }
            return this.matches;
        }
    }

    public static interface ProgressCallback {

        void searchStarted(int pageIndex);

        void searchFinished(int pageIndex);

        boolean isCancelled();
    }
}
