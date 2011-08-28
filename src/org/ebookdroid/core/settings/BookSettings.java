package org.ebookdroid.core.settings;

import org.ebookdroid.core.PageAlign;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.curl.PageAnimationType;
import org.ebookdroid.core.events.CurrentPageListener;
import org.ebookdroid.utils.CompareUtils;

import java.util.ArrayList;
import java.util.List;

public class BookSettings implements CurrentPageListener {

    final String fileName;

    long lastUpdated;

    PageIndex currentPage;

    int zoom = 100;

    boolean splitPages;

    boolean singlePage;

    PageAlign pageAlign = PageAlign.AUTO;

    PageAnimationType animationType = PageAnimationType.NONE;

    final List<Bookmark> bookmarks = new ArrayList<Bookmark>();

    BookSettings(BookSettings current) {
        this.fileName = current.fileName;
        this.currentPage = current.currentPage;
        this.lastUpdated = current.lastUpdated;
        this.zoom = current.zoom;
        this.splitPages = current.splitPages;
        this.singlePage = current.singlePage;
        this.pageAlign = current.pageAlign;
        this.animationType = current.animationType;
        this.bookmarks.addAll(current.bookmarks);

    }

    BookSettings(final String fileName, AppSettings appSettings) {
        this.fileName = fileName;
        this.currentPage = PageIndex.FIRST;
        this.lastUpdated = System.currentTimeMillis();
        if (appSettings != null) {
            appSettings.fillBookSettings(this);
        }
    }

    @Override
    public void currentPageChanged(PageIndex oldIndex, PageIndex newIndex) {
        this.currentPage = newIndex;
    }

    public String getFileName() {
        return fileName;
    }

    public PageIndex getCurrentPage() {
        return currentPage;
    }

    public float getZoom() {
        return zoom / 100.0f;
    }

    void setZoom(float zoom) {
        this.zoom = Math.round(zoom * 100);
    }

    public boolean getSinglePage() {
        return singlePage;
    }

    public PageAlign getPageAlign() {
        return pageAlign;
    }

    public boolean getSplitPages() {
        return splitPages;
    }

    public PageAnimationType getAnimationType() {
        return animationType;
    }

    public List<Bookmark> getBookmarks() {
        return bookmarks;
    }

    public static class Diff {

        private static final short D_CurrentPage = 0x0001 << 0;
        private static final short D_Zoom = 0x0001 << 1;
        private static final short D_SplitPages = 0x0001 << 2;
        private static final short D_SinglePage = 0x0001 << 3;
        private static final short D_PageAlign = 0x0001 << 4;
        private static final short D_AnimationType = 0x0001 << 5;

        private short mask;
        private final boolean firstTime;

        public Diff(BookSettings olds, BookSettings news) {
            firstTime = olds == null;
            if (news != null) {
                if (firstTime || !CompareUtils.equals(olds.currentPage, news.currentPage)) {
                    mask |= D_CurrentPage;
                }
                if (firstTime || olds.zoom != news.zoom) {
                    mask |= D_Zoom;
                }
                if (firstTime || olds.splitPages != news.splitPages) {
                    mask |= D_SplitPages;
                }
                if (firstTime || olds.singlePage != news.singlePage) {
                    mask |= D_SinglePage;
                }
                if (firstTime || olds.pageAlign != news.pageAlign) {
                    mask |= D_PageAlign;
                }
                if (firstTime || olds.animationType != news.animationType) {
                    mask |= D_AnimationType;
                }
            }
        }

        public boolean isFirstTime() {
            return firstTime;
        }

        public boolean isCurrentPageChanged() {
            return 0 != (mask & D_CurrentPage);
        }

        public boolean isZoomChanged() {
            return 0 != (mask & D_Zoom);
        }

        public boolean isSplitPagesChanged() {
            return 0 != (mask & D_SplitPages);
        }

        public boolean isSinglePageChanged() {
            return 0 != (mask & D_SinglePage);
        }

        public boolean isPageAlignChanged() {
            return 0 != (mask & D_PageAlign);
        }

        public boolean isAnimationTypeChanged() {
            return 0 != (mask & D_AnimationType);
        }
    }
}
