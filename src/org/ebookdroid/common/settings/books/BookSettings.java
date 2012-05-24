package org.ebookdroid.common.settings.books;

import org.ebookdroid.common.settings.definitions.AppPreferences;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.curl.PageAnimationType;
import org.ebookdroid.core.events.CurrentPageListener;

import java.util.ArrayList;
import java.util.List;

public class BookSettings implements CurrentPageListener {

    public boolean persistent;

    public final String fileName;

    public long lastUpdated;

    public PageIndex currentPage;

    public int zoom = 100;

    public boolean splitPages;

    public DocumentViewMode viewMode;

    public PageAlign pageAlign = PageAlign.AUTO;

    public PageAnimationType animationType = PageAnimationType.NONE;

    public final List<Bookmark> bookmarks = new ArrayList<Bookmark>();

    public boolean cropPages;

    public float offsetX;

    public float offsetY;

    public boolean nightMode;

    public int contrast = AppPreferences.CONTRAST.defValue;

    public int exposure = AppPreferences.EXPOSURE.defValue;

    public boolean autoLevels;

    public BookSettings(BookSettings current) {
        this.persistent = current.persistent;
        this.fileName = current.fileName;
        this.lastUpdated = current.lastUpdated;

        this.currentPage = current.currentPage;
        this.zoom = current.zoom;
        this.splitPages = current.splitPages;
        this.viewMode = current.viewMode;
        this.pageAlign = current.pageAlign;
        this.animationType = current.animationType;
        this.bookmarks.addAll(current.bookmarks);
        this.cropPages = current.cropPages;
        this.offsetX = current.offsetX;
        this.offsetY = current.offsetY;
        this.nightMode = current.nightMode;
        this.contrast = current.contrast;
        this.exposure = current.exposure;
        this.autoLevels = current.autoLevels;
    }

    public BookSettings(String fileName, BookSettings current) {
        this.persistent = true;
        this.fileName = fileName;
        this.lastUpdated = current.lastUpdated;

        this.currentPage = current.currentPage;
        this.zoom = current.zoom;
        this.splitPages = current.splitPages;
        this.viewMode = current.viewMode;
        this.pageAlign = current.pageAlign;
        this.animationType = current.animationType;
        this.bookmarks.addAll(current.bookmarks);
        this.cropPages = current.cropPages;
        this.offsetX = current.offsetX;
        this.offsetY = current.offsetY;
        this.nightMode = current.nightMode;
        this.contrast = current.contrast;
        this.exposure = current.exposure;
        this.autoLevels = current.autoLevels;
    }

    public BookSettings(final String fileName) {
        this.persistent = true;
        this.fileName = fileName;
        this.lastUpdated = System.currentTimeMillis();
        this.currentPage = PageIndex.FIRST;
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

        private static final short D_SplitPages = 0x0001 << 2;
        private static final short D_ViewMode = 0x0001 << 3;
        private static final short D_PageAlign = 0x0001 << 4;
        private static final short D_AnimationType = 0x0001 << 5;
        private static final short D_CropPages = 0x0001 << 6;
        private static final short D_Contrast = 0x0001 << 7;
        private static final short D_Exposure = 0x0001 << 8;
        private static final short D_NightMode = 0x0001 << 9;
        private static final short D_AutoLevels = 0x0001 << 10;

        private static final short D_Effects = D_Contrast | D_Exposure | D_NightMode | D_AutoLevels;

        private short mask;
        private final boolean firstTime;

        public Diff(BookSettings olds, BookSettings news) {
            firstTime = olds == null;
            if (firstTime) {
                mask = (short) 0xFFFF;
            } else if (news != null) {
                if (olds.splitPages != news.splitPages) {
                    mask |= D_SplitPages;
                }
                if (olds.cropPages != news.cropPages) {
                    mask |= D_CropPages;
                }
                if (olds.viewMode != news.viewMode) {
                    mask |= D_ViewMode;
                }
                if (olds.pageAlign != news.pageAlign) {
                    mask |= D_PageAlign;
                }
                if (olds.animationType != news.animationType) {
                    mask |= D_AnimationType;
                }
                if (olds.contrast != news.contrast) {
                    mask |= D_Contrast;
                }
                if (olds.exposure != news.exposure) {
                    mask |= D_Exposure;
                }
                if (olds.nightMode != news.nightMode) {
                    mask |= D_NightMode;
                }
                if (olds.autoLevels != news.autoLevels) {
                    mask |= D_AutoLevels;
                }
            }
        }

        public boolean isFirstTime() {
            return firstTime;
        }

        public boolean isSplitPagesChanged() {
            return 0 != (mask & D_SplitPages);
        }

        public boolean isViewModeChanged() {
            return 0 != (mask & D_ViewMode);
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

        public boolean isContrastChanged() {
            return 0 != (mask & D_Contrast);
        }

        public boolean isExposureChanged() {
            return 0 != (mask & D_Exposure);
        }

        public boolean isNightModeChanged() {
            return 0 != (mask & D_NightMode);
        }

        public boolean isAutoLevelsChanged() {
            return 0 != (mask & D_AutoLevels);
        }

        public boolean isEffectsChanged() {
            return 0 != (mask & (D_Effects));
        }
    }
}
