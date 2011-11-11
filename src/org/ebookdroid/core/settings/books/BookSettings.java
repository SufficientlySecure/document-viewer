package org.ebookdroid.core.settings.books;

import org.ebookdroid.core.PageAlign;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.curl.PageAnimationType;
import org.ebookdroid.core.events.CurrentPageListener;
import org.ebookdroid.utils.CompareUtils;

import java.util.ArrayList;
import java.util.List;

public class BookSettings implements CurrentPageListener {

    public final String fileName;

    public long lastUpdated;

    public PageIndex currentPage;

    public int zoom = 100;

    public boolean splitPages;

    public boolean singlePage;

    public PageAlign pageAlign = PageAlign.AUTO;

    public PageAnimationType animationType = PageAnimationType.NONE;

    public final List<Bookmark> bookmarks = new ArrayList<Bookmark>();

    public boolean cropPages;

    public float offsetX;

    public float offsetY;

    public BookSettings(BookSettings current) {
        this.fileName = current.fileName;
        this.currentPage = current.currentPage;
        this.lastUpdated = current.lastUpdated;
        this.zoom = current.zoom;
        this.splitPages = current.splitPages;
        this.singlePage = current.singlePage;
        this.pageAlign = current.pageAlign;
        this.animationType = current.animationType;
        this.bookmarks.addAll(current.bookmarks);
        this.cropPages = current.cropPages;
        this.offsetX = current.offsetX;
        this.offsetY = current.offsetY;
    }

    public BookSettings(final String fileName) {
        this.fileName = fileName;
        this.currentPage = PageIndex.FIRST;
        this.lastUpdated = System.currentTimeMillis();
    }

    @Override
    public void currentPageChanged(PageIndex oldIndex, PageIndex newIndex) {
        this.currentPage = newIndex;
    }

    public PageIndex getCurrentPage() {
        return currentPage;
    }

    public float getZoom() {
        return zoom / 100.0f;
    }

    public void setZoom(float zoom) {
        this.zoom = Math.round(zoom * 100);
    }

    public static class Diff {

        private static final short D_CurrentPage = 0x0001 << 0;
        private static final short D_Zoom = 0x0001 << 1;
        private static final short D_SplitPages = 0x0001 << 2;
        private static final short D_SinglePage = 0x0001 << 3;
        private static final short D_PageAlign = 0x0001 << 4;
        private static final short D_AnimationType = 0x0001 << 5;
        private static final short D_CropPages = 0x0001 << 6;

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
                if (firstTime || olds.cropPages != news.cropPages) {
                    mask |= D_CropPages;
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

        public boolean isCropPagesChanged() {
            return 0 != (mask & D_CropPages);
        }
    }
}
