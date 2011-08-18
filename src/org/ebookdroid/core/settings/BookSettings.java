package org.ebookdroid.core.settings;

import org.ebookdroid.core.PageAlign;
import org.ebookdroid.core.curl.PageAnimationType;
import org.ebookdroid.core.events.CurrentPageListener;

public class BookSettings implements CurrentPageListener {

    final String fileName;

    long lastUpdated;

    int currentDocPage;

    int currentViewPage;

    int zoom = 100;

    boolean splitPages;

    boolean singlePage;

    PageAlign pageAlign = PageAlign.AUTO;

    PageAnimationType animationType = PageAnimationType.NONE;

    BookSettings(final String fileName, AppSettings appSettings) {
        this.fileName = fileName;
        this.lastUpdated = System.currentTimeMillis();
        if (appSettings != null) {
            appSettings.fillBookSettings(this);
        }
    }

    BookSettings(final BookSettings old, AppSettings appSettings) {
        this(old.fileName, appSettings);
        this.currentDocPage = old.currentDocPage;
        this.currentViewPage = old.currentViewPage;
        this.zoom = old.zoom;
    }

    @Override
    public void currentPageChanged(int docPageIndex, int viewPageIndex) {
        this.currentDocPage = docPageIndex;
        this.currentViewPage = viewPageIndex;
    }

    public String getFileName() {
        return fileName;
    }

    public int getCurrentDocPage() {
        return currentDocPage;
    }

    public int getCurrentViewPage() {
        return currentViewPage;
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

    public static class Diff {

        private static final short D_CurrentDocPage = 0x0001 << 0;
        private static final short D_CurrentViewPage = 0x0001 << 1;
        private static final short D_Zoom = 0x0001 << 2;
        private static final short D_SplitPages = 0x0001 << 3;
        private static final short D_SinglePage = 0x0001 << 4;
        private static final short D_PageAlign = 0x0001 << 5;
        private static final short D_AnimationType = 0x0001 << 6;

        private short mask;
        private final boolean firstTime;

        public Diff(BookSettings olds, BookSettings news) {
            firstTime = olds == null;
            if (news != null) {
                if (firstTime || olds.getCurrentDocPage() != news.getCurrentDocPage()) {
                    mask |= D_CurrentDocPage;
                }
                if (firstTime || olds.getCurrentViewPage() != news.getCurrentViewPage()) {
                    mask |= D_CurrentViewPage;
                }
                if (firstTime || olds.getZoom() != news.getZoom()) {
                    mask |= D_Zoom;
                }
                if (firstTime || olds.getSplitPages() != news.getSplitPages()) {
                    mask |= D_SplitPages;
                }
                if (firstTime || olds.getSinglePage() != news.getSinglePage()) {
                    mask |= D_SinglePage;
                }
                if (firstTime || olds.getPageAlign() != news.getPageAlign()) {
                    mask |= D_PageAlign;
                }
                if (firstTime || olds.getAnimationType() != news.getAnimationType()) {
                    mask |= D_AnimationType;
                }
            }
        }

        public boolean isFirstTime() {
            return firstTime;
        }

        public boolean isCurrentDocPageChanged() {
            return 0 != (mask & D_CurrentDocPage);
        }

        public boolean isCurrentViewPageChanged() {
            return 0 != (mask & D_CurrentViewPage);
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
