package org.ebookdroid.core.settings;

import org.ebookdroid.core.PageAlign;
import org.ebookdroid.core.RotationType;
import org.ebookdroid.core.curl.PageAnimationType;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class AppSettings {

    private final SharedPreferences prefs;

    private Boolean tapScroll;

    private Integer tapSize;

    private Boolean singlePage;

    private Integer pagesInMemory;

    private Boolean nightMode;

    private PageAlign pageAlign;

    private Boolean fullScreen;

    private RotationType rotation;

    private Boolean showTitle;

    private Integer scrollHeight;

    private Boolean sliceLimit;

    private PageAnimationType animationType;

    private Boolean splitPages;

    private Boolean pageInTitle;

    private Integer brightness;
    
    AppSettings(final Context context) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public int getBrightness() {
        if (brightness == null) {
            brightness = getIntValue("brightness", 100);
        }
        return brightness;
    }

    public boolean getNightMode() {
        if (nightMode == null) {
            nightMode = prefs.getBoolean("nightmode", false);
        }
        return nightMode;
    }

    public void switchNightMode() {
        nightMode = !nightMode;
        final Editor edit = prefs.edit();
        edit.putBoolean("nightmode", nightMode);
        edit.commit();
    }

    public RotationType getRotation() {
        if (rotation == null) {
            final String rotationStr = prefs.getString("rotation", RotationType.AUTOMATIC.getResValue());
            rotation = RotationType.getByResValue(rotationStr);
            if (rotation == null) {
                rotation = RotationType.AUTOMATIC;
            }
        }
        return rotation;
    }

    public boolean getFullScreen() {
        if (fullScreen == null) {
            fullScreen = prefs.getBoolean("fullscreen", false);
        }
        return fullScreen;
    }

    public boolean getShowTitle() {
        if (showTitle == null) {
            showTitle = prefs.getBoolean("title", true);
        }
        return showTitle;
    }

    public boolean getPageInTitle() {
        if (pageInTitle == null) {
            pageInTitle = prefs.getBoolean("pageintitle", true);
        }
        return pageInTitle;
    }

    public boolean getTapScroll() {
        if (tapScroll == null) {
            tapScroll = prefs.getBoolean("tapscroll", false);
        }
        return tapScroll;
    }

    public int getTapSize() {
        if (tapSize == null) {
            tapSize = getIntValue("tapsize", 10);
        }
        return tapSize.intValue();
    }

    public int getScrollHeight() {
        if (scrollHeight == null) {
            scrollHeight = getIntValue("scrollheight", 50);
        }
        return scrollHeight.intValue();
    }

    public int getPagesInMemory() {
        if (pagesInMemory == null) {
            pagesInMemory = getIntValue("pagesinmemory", 2);
        }
        return pagesInMemory.intValue();
    }

    public Boolean getSliceLimit() {
        if (sliceLimit == null) {
            sliceLimit = prefs.getBoolean("slicelimit", true);
        }
        return sliceLimit;
    }

    boolean getSplitPages() {
        if (splitPages == null) {
            splitPages = prefs.getBoolean("splitpages", false);
        }
        return splitPages;
    }

    boolean getSinglePage() {
        if (singlePage == null) {
            singlePage = prefs.getBoolean("singlepage", false);
        }
        return singlePage;
    }

    PageAlign getPageAlign() {
        if (pageAlign == null) {
            final String align = prefs.getString("align", PageAlign.AUTO.getResValue());
            pageAlign = PageAlign.getByResValue(align);
            if (pageAlign == null) {
                pageAlign = PageAlign.AUTO;
            }
        }
        return pageAlign;
    }

    PageAnimationType getAnimationType() {
        if (animationType == null) {
            animationType = PageAnimationType.get(prefs.getString("animationType", null));
        }
        return animationType;
    }

    void clearPseudoBookSettings() {
        final Editor editor = prefs.edit();
        editor.remove("book");
        editor.remove("book_splitpages");
        editor.remove("book_singlepage");
        editor.remove("book_align");
        editor.remove("book_animationType");
        editor.commit();
    }

    void updatePseudoBookSettings(final BookSettings bs) {
        final Editor editor = prefs.edit();
        editor.putString("book", bs.getFileName());
        editor.putBoolean("book_splitpages", bs.getSplitPages());
        editor.putBoolean("book_singlepage", bs.getSinglePage());
        editor.putString("book_align", bs.getPageAlign().getResValue());
        editor.putString("book_animationType", bs.getAnimationType().getResValue());
        editor.commit();
    }

    void fillBookSettings(final BookSettings bs) {
        bs.splitPages = prefs.getBoolean("book_splitpages", getSplitPages());
        bs.singlePage = prefs.getBoolean("book_singlepage", getSinglePage());

        bs.pageAlign = PageAlign.getByResValue(prefs.getString("book_align", getPageAlign().getResValue()));
        if (bs.pageAlign == null) {
            bs.pageAlign = PageAlign.AUTO;
        }
        bs.animationType = PageAnimationType.get(prefs
                .getString("book_animationType", getAnimationType().getResValue()));
        if (bs.animationType == null) {
            bs.animationType = PageAnimationType.NONE;
        }

    }

    private int getIntValue(final String key, final int defaultValue) {
        final String str = prefs.getString(key, "" + defaultValue);
        int value = defaultValue;
        try {
            value = Integer.parseInt(str);
        } catch (final NumberFormatException e) {
        }
        return value;
    }

    public static class Diff {

        private static final short D_NightMode = 0x0001 << 0;
        private static final short D_Rotation = 0x0001 << 1;
        private static final short D_FullScreen = 0x0001 << 2;
        private static final short D_ShowTitle = 0x0001 << 3;
        private static final short D_PageInTitle = 0x0001 << 4;
        private static final short D_TapScroll = 0x0001 << 5;
        private static final short D_TapSize = 0x0001 << 6;
        private static final short D_ScrollHeight = 0x0001 << 7;
        private static final short D_PagesInMemory = 0x0001 << 8;
        private static final short D_SliceLimit = 0x0001 << 9;

        private short mask;
        private final boolean firstTime;

        public Diff(AppSettings olds, AppSettings news) {
            firstTime = olds == null;
            if (news != null) {
                if (firstTime || olds.getNightMode() != news.getNightMode()) {
                    mask |= D_NightMode;
                }
                if (firstTime || olds.getRotation() != news.getRotation()) {
                    mask |= D_Rotation;
                }
                if (firstTime || olds.getFullScreen() != news.getFullScreen()) {
                    mask |= D_FullScreen;
                }
                if (firstTime || olds.getShowTitle() != news.getShowTitle()) {
                    mask |= D_ShowTitle;
                }
                if (firstTime || olds.getPageInTitle() != news.getPageInTitle()) {
                    mask |= D_PageInTitle;
                }
                if (firstTime || olds.getTapScroll() != news.getTapScroll()) {
                    mask |= D_TapScroll;
                }
                if (firstTime || olds.getTapSize() != news.getTapSize()) {
                    mask |= D_TapSize;
                }
                if (firstTime || olds.getScrollHeight() != news.getScrollHeight()) {
                    mask |= D_ScrollHeight;
                }
                if (firstTime || olds.getPagesInMemory() != news.getPagesInMemory()) {
                    mask |= D_PagesInMemory;
                }
                if (firstTime || olds.getSliceLimit() != news.getSliceLimit()) {
                    mask |= D_SliceLimit;
                }
            }
        }

        public boolean isFirstTime() {
            return firstTime;
        }

        public boolean isNightModeChanged() {
            return 0 != (mask & D_NightMode);
        }

        public boolean isRotationChanged() {
            return 0 != (mask & D_Rotation);
        }

        public boolean isFullScreenChanged() {
            return 0 != (mask & D_FullScreen);
        }

        public boolean isShowTitleChanged() {
            return 0 != (mask & D_ShowTitle);
        }

        public boolean isPageInTitleChanged() {
            return 0 != (mask & D_PageInTitle);
        }

        public boolean isTapScrollChanged() {
            return 0 != (mask & D_TapScroll);
        }

        public boolean isTapSizeChanged() {
            return 0 != (mask & D_TapSize);
        }

        public boolean isScrollHeightChanged() {
            return 0 != (mask & D_ScrollHeight);
        }

        public boolean isPagesInMemoryChanged() {
            return 0 != (mask & D_PagesInMemory);
        }

        public boolean isSliceLimitChanged() {
            return 0 != (mask & D_SliceLimit);
        }
    }

}
