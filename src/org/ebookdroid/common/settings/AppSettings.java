package org.ebookdroid.common.settings;

import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.definitions.AppPreferences;
import org.ebookdroid.common.settings.definitions.BookPreferences;
import org.ebookdroid.common.settings.listeners.IAppSettingsChangeListener;
import org.ebookdroid.common.settings.types.BookRotationType;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.settings.types.FontSize;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.common.settings.types.RotationType;
import org.ebookdroid.common.settings.types.ToastPosition;
import org.ebookdroid.core.curl.PageAnimationType;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.emdev.common.backup.BackupManager;
import org.emdev.common.backup.IBackupAgent;
import org.emdev.common.settings.backup.SettingsBackupHelper;
import org.emdev.common.xml.XmlParsers;
import org.emdev.utils.CompareUtils;
import org.json.JSONObject;

public class AppSettings implements AppPreferences, BookPreferences, IBackupAgent {

    public static final String BACKUP_KEY = "app-settings";

    private static AppSettings current;

    /* =============== UI settings =============== */

    public final String lang;

    public final boolean loadRecent;

    public final boolean confirmClose;

    public final boolean brightnessInNightModeOnly;

    public final int brightness;

    public final boolean keepScreenOn;

    public final RotationType rotation;

    public final boolean fullScreen;

    public final boolean showTitle;

    public final boolean pageInTitle;

    public final ToastPosition pageNumberToastPosition;

    public final ToastPosition zoomToastPosition;

    public final boolean showAnimIcon;

    /* =============== Tap & Scroll settings =============== */

    public final boolean tapsEnabled;

    public final int scrollHeight;

    public final boolean animateScrolling;

    /* =============== Tap & Keyboard settings =============== */

    public final String tapProfiles;

    public final String keysBinding;

    /* =============== Navigation & History settings =============== */

    public final boolean showBookmarksInMenu;

    public final int linkHighlightColor;

    public final int searchHighlightColor;

    public final int currentSearchHighlightColor;

    public final boolean storeGotoHistory;

    public final boolean storeLinkGotoHistory;

    public final boolean storeOutlineGotoHistory;

    public final boolean storeSearchGotoHistory;

    /* =============== Performance settings =============== */

    public final int pagesInMemory;

    public final int decodingThreads;

    public final int decodingThreadPriority;

    public final int drawThreadPriority;

    public final boolean decodingOnScroll;

    public final int bitmapSize;

    public final int heapPreallocate;

    public final int pdfStorageSize;

    /* =============== Default rendering settings =============== */

    public final boolean nightMode;

    public boolean positiveImagesInNightMode;

    final int contrast;

    final int gamma;

    final int exposure;

    final boolean autoLevels;

    final boolean splitPages;

    final boolean splitRTL;

    final boolean cropPages;

    public final DocumentViewMode viewMode;

    final PageAlign pageAlign;

    final PageAnimationType animationType;

    /* =============== DjVU Format-specific settings =============== */

    public final int djvuRenderingMode;

    /* =============== PDF Format-specific settings =============== */

    public final boolean useCustomDpi;

    public final int xDpi;

    public final int yDpi;

    public final String monoFontPack;

    public final String sansFontPack;

    public final String serifFontPack;

    public final String symbolFontPack;

    public final String dingbatFontPack;

    public final boolean slowCMYK;

    /* =============== FB2 Format-specific settings =============== */

    public final XmlParsers fb2XmlParser;

    public final String fb2FontPack;

    public final FontSize fontSize;

    public final boolean fb2HyphenEnabled;

    public final boolean fb2CacheImagesOnDisk;

    /* =============================================== */

    private AppSettings() {
        BackupManager.addAgent(this);
        final SharedPreferences prefs = SettingsManager.prefs;
        /* =============== UI settings =============== */
        lang = LANG.getPreferenceValue(prefs);
        loadRecent = LOAD_RECENT.getPreferenceValue(prefs);
        confirmClose = CONFIRM_CLOSE.getPreferenceValue(prefs);
        brightnessInNightModeOnly = BRIGHTNESS_NIGHT_MODE_ONLY.getPreferenceValue(prefs);
        brightness = BRIGHTNESS.getPreferenceValue(prefs);
        keepScreenOn = KEEP_SCREEN_ON.getPreferenceValue(prefs);
        rotation = ROTATION.getPreferenceValue(prefs);
        fullScreen = FULLSCREEN.getPreferenceValue(prefs);
        showTitle = SHOW_TITLE.getPreferenceValue(prefs);
        pageInTitle = SHOW_PAGE_IN_TITLE.getPreferenceValue(prefs);
        pageNumberToastPosition = PAGE_NUMBER_TOAST_POSITION.getPreferenceValue(prefs);
        zoomToastPosition = ZOOM_TOAST_POSITION.getPreferenceValue(prefs);
        showAnimIcon = SHOW_ANIM_ICON.getPreferenceValue(prefs);
        /* =============== Tap & Scroll settings =============== */
        tapsEnabled = TAPS_ENABLED.getPreferenceValue(prefs);
        scrollHeight = SCROLL_HEIGHT.getPreferenceValue(prefs);
        animateScrolling = ANIMATE_SCROLLING.getPreferenceValue(prefs);
        /* =============== Navigation & History settings =============== */
        showBookmarksInMenu = SHOW_BOOKMARKs_MENU.getPreferenceValue(prefs);
        linkHighlightColor = LINK_HIGHLIGHT_COLOR.getPreferenceValue(prefs);
        searchHighlightColor = SEARCH_HIGHLIGHT_COLOR.getPreferenceValue(prefs);
        currentSearchHighlightColor = CURRENT_SEARCH_HIGHLIGHT_COLOR.getPreferenceValue(prefs);
        storeGotoHistory = STORE_GOTO_HISTORY.getPreferenceValue(prefs);
        storeLinkGotoHistory = STORE_LINK_GOTO_HISTORY.getPreferenceValue(prefs);
        storeOutlineGotoHistory = STORE_OUTLINE_GOTO_HISTORY.getPreferenceValue(prefs);
        storeSearchGotoHistory = STORE_SEARCH_GOTO_HISTORY.getPreferenceValue(prefs);
        /* =============== Tap & Keyboard settings =============== */
        tapProfiles = TAP_PROFILES.getPreferenceValue(prefs);
        keysBinding = KEY_BINDINGS.getPreferenceValue(prefs);
        /* =============== Performance settings =============== */
        pagesInMemory = PAGES_IN_MEMORY.getPreferenceValue(prefs);
        decodingThreads = DECODING_THREADS.getPreferenceValue(prefs);
        decodingThreadPriority = DECODE_THREAD_PRIORITY.getPreferenceValue(prefs);
        drawThreadPriority = DRAW_THREAD_PRIORITY.getPreferenceValue(prefs);
        decodingOnScroll = DECODING_ON_SCROLL.getPreferenceValue(prefs);
        bitmapSize = BITMAP_SIZE.getPreferenceValue(prefs);
        heapPreallocate = HEAP_PREALLOCATE.getPreferenceValue(prefs);
        pdfStorageSize = PDF_STORAGE_SIZE.getPreferenceValue(prefs);
        /* =============== Default rendering settings =============== */
        nightMode = NIGHT_MODE.getPreferenceValue(prefs);
        positiveImagesInNightMode = NIGHT_MODE_POS_IMAGES.getPreferenceValue(prefs);
        contrast = CONTRAST.getPreferenceValue(prefs);
        gamma = GAMMA.getPreferenceValue(prefs);
        exposure = EXPOSURE.getPreferenceValue(prefs);
        autoLevels = AUTO_LEVELS.getPreferenceValue(prefs);
        splitPages = SPLIT_PAGES.getPreferenceValue(prefs);
        splitRTL = SPLIT_RTL.getPreferenceValue(prefs);
        cropPages = CROP_PAGES.getPreferenceValue(prefs);
        viewMode = VIEW_MODE.getPreferenceValue(prefs);
        pageAlign = PAGE_ALIGN.getPreferenceValue(prefs);
        animationType = ANIMATION_TYPE.getPreferenceValue(prefs);
        /* =============== DjVU Format-specific settings =============== */
        djvuRenderingMode = DJVU_RENDERING_MODE.getPreferenceValue(prefs);
        /* =============== PDF Format-specific settings =============== */
        useCustomDpi = PDF_CUSTOM_DPI.getPreferenceValue(prefs);
        xDpi = PDF_CUSTOM_XDPI.getPreferenceValue(prefs);
        yDpi = PDF_CUSTOM_YDPI.getPreferenceValue(prefs);
        monoFontPack = MONO_FONT_PACK.getPreferenceValue(prefs);
        sansFontPack = SANS_FONT_PACK.getPreferenceValue(prefs);
        serifFontPack = SERIF_FONT_PACK.getPreferenceValue(prefs);
        symbolFontPack = SYMBOL_FONT_PACK.getPreferenceValue(prefs);
        dingbatFontPack = DINGBAT_FONT_PACK.getPreferenceValue(prefs);
        slowCMYK = PDF_SLOW_CMYK.getPreferenceValue(prefs);
        /* =============== FB2 Format-specific settings =============== */
        fb2XmlParser = FB2_XML_PARSER.getPreferenceValue(prefs);
        fb2FontPack = FB2_FONT_PACK.getPreferenceValue(prefs);
        fontSize = FB2_FONT_SIZE.getPreferenceValue(prefs);
        fb2HyphenEnabled = FB2_HYPHEN.getPreferenceValue(prefs);
        fb2CacheImagesOnDisk = FB2_CACHE_IMAGES.getPreferenceValue(prefs);
    }

    /* =============== UI settings =============== */
    /* =============== Tap & Scroll settings =============== */
    /* =============== Tap & Keyboard settings =============== */
    /* =============== Default rendering settings =============== */
    /* =============== Format-specific settings =============== */

    public float getXDpi(final float def) {
        return useCustomDpi ? xDpi : def;
    }

    public float getYDpi(final float def) {
        return useCustomDpi ? yDpi : def;
    }

    /* =============== */

    public static void init() {
        current = new AppSettings();
    }

    public static AppSettings current() {
        SettingsManager.lock.readLock().lock();
        try {
            return current;
        } finally {
            SettingsManager.lock.readLock().unlock();
        }
    }

    public static void toggleFullScreen() {
        SettingsManager.lock.writeLock().lock();
        try {
            final Editor edit = SettingsManager.prefs.edit();
            AppPreferences.FULLSCREEN.setPreferenceValue(edit, !current.fullScreen);
            edit.commit();
            onSettingsChanged();
        } finally {
            SettingsManager.lock.writeLock().unlock();
        }
    }

    public static void toggleTitleVisibility() {
        SettingsManager.lock.writeLock().lock();
        try {
            final Editor edit = SettingsManager.prefs.edit();
            AppPreferences.SHOW_TITLE.setPreferenceValue(edit, !current.showTitle);
            edit.commit();
            onSettingsChanged();
        } finally {
            SettingsManager.lock.writeLock().unlock();
        }
    }

    public static void updateTapProfiles(final String profiles) {
        SettingsManager.lock.writeLock().lock();
        try {
            final Editor edit = SettingsManager.prefs.edit();
            AppPreferences.TAP_PROFILES.setPreferenceValue(edit, profiles);
            edit.commit();
            onSettingsChanged();
        } finally {
            SettingsManager.lock.writeLock().unlock();
        }
    }

    public static void updateKeysBinding(final String json) {
        SettingsManager.lock.writeLock().lock();
        try {
            final Editor edit = SettingsManager.prefs.edit();
            AppPreferences.KEY_BINDINGS.setPreferenceValue(edit, json);
            edit.commit();
            onSettingsChanged();
        } finally {
            SettingsManager.lock.writeLock().unlock();
        }
    }

    /* =============== */

    static void setDefaultSettings(final BookSettings bs) {
        bs.nightMode = current.nightMode;
        bs.positiveImagesInNightMode = current.positiveImagesInNightMode;
        bs.contrast = current.contrast;
        bs.gamma = current.gamma;
        bs.exposure = current.exposure;
        bs.autoLevels = current.autoLevels;
        bs.splitPages = current.splitPages;
        bs.splitRTL = current.splitRTL;
        bs.cropPages = current.cropPages;
        bs.viewMode = current.viewMode;
        bs.pageAlign = current.pageAlign;
        bs.animationType = current.animationType;
    }

    static void fillBookSettings(final BookSettings bs) {
        final SharedPreferences prefs = SettingsManager.prefs;
        bs.nightMode = BOOK_NIGHT_MODE.getPreferenceValue(prefs, current.nightMode);
        bs.positiveImagesInNightMode = BOOK_NIGHT_MODE_POS_IMAGES.getPreferenceValue(prefs, current.positiveImagesInNightMode);
        bs.contrast = BOOK_CONTRAST.getPreferenceValue(prefs, current.contrast);
        bs.gamma = BOOK_GAMMA.getPreferenceValue(prefs, current.gamma);
        bs.exposure = BOOK_EXPOSURE.getPreferenceValue(prefs, current.exposure);
        bs.autoLevels = BOOK_AUTO_LEVELS.getPreferenceValue(prefs, current.autoLevels);
        bs.splitPages = BOOK_SPLIT_PAGES.getPreferenceValue(prefs, current.splitPages);
        bs.splitRTL = BOOK_SPLIT_RTL.getPreferenceValue(prefs, current.splitRTL);
        bs.cropPages = BOOK_CROP_PAGES.getPreferenceValue(prefs, current.cropPages);
        bs.rotation = BOOK_ROTATION.getPreferenceValue(prefs, BookRotationType.UNSPECIFIED);
        bs.viewMode = BOOK_VIEW_MODE.getPreferenceValue(prefs, current.viewMode);
        bs.pageAlign = BOOK_PAGE_ALIGN.getPreferenceValue(prefs, current.pageAlign);
        bs.animationType = BOOK_ANIMATION_TYPE.getPreferenceValue(prefs, current.animationType);
        bs.firstPageOffset = BOOK_FIRST_PAGE_OFFSET.getPreferenceValue(prefs);
    }

    static void clearPseudoBookSettings() {
        final SharedPreferences prefs = SettingsManager.prefs;
        final Editor edit = prefs.edit();
        edit.remove(BOOK.key);
        edit.remove(BOOK_NIGHT_MODE.key);
        edit.remove(BOOK_NIGHT_MODE_POS_IMAGES.key);
        edit.remove(BOOK_CONTRAST.key);
        edit.remove(BOOK_GAMMA.key);
        edit.remove(BOOK_EXPOSURE.key);
        edit.remove(BOOK_AUTO_LEVELS.key);
        edit.remove(BOOK_SPLIT_PAGES.key);
        edit.remove(BOOK_SPLIT_RTL.key);
        edit.remove(BOOK_CROP_PAGES.key);
        edit.remove(BOOK_ROTATION.key);
        edit.remove(BOOK_VIEW_MODE.key);
        edit.remove(BOOK_PAGE_ALIGN.key);
        edit.remove(BOOK_ANIMATION_TYPE.key);
        edit.remove(BOOK_FIRST_PAGE_OFFSET.key);
        edit.commit();
    }

    static void updatePseudoBookSettings(final BookSettings bs) {
        final SharedPreferences prefs = SettingsManager.prefs;
        final Editor edit = prefs.edit();
        BOOK.setPreferenceValue(edit, bs.fileName);
        BOOK_NIGHT_MODE.setPreferenceValue(edit, bs.nightMode);
        BOOK_NIGHT_MODE_POS_IMAGES.setPreferenceValue(edit, bs.positiveImagesInNightMode);
        BOOK_CONTRAST.setPreferenceValue(edit, bs.contrast);
        BOOK_GAMMA.setPreferenceValue(edit, bs.gamma);
        BOOK_EXPOSURE.setPreferenceValue(edit, bs.exposure);
        BOOK_AUTO_LEVELS.setPreferenceValue(edit, bs.autoLevels);
        BOOK_SPLIT_PAGES.setPreferenceValue(edit, bs.splitPages);
        BOOK_SPLIT_RTL.setPreferenceValue(edit, bs.splitRTL);
        BOOK_CROP_PAGES.setPreferenceValue(edit, bs.cropPages);
        BOOK_ROTATION.setPreferenceValue(edit, bs.rotation);
        BOOK_VIEW_MODE.setPreferenceValue(edit, bs.viewMode);
        BOOK_PAGE_ALIGN.setPreferenceValue(edit, bs.pageAlign);
        BOOK_ANIMATION_TYPE.setPreferenceValue(edit, bs.animationType);
        BOOK_FIRST_PAGE_OFFSET.setPreferenceValue(edit, bs.firstPageOffset);
        edit.commit();
    }

    static Diff onSettingsChanged() {
        final AppSettings oldAppSettings = current;
        current = new AppSettings();
        return applySettingsChanges(oldAppSettings, current);
    }

    static AppSettings.Diff applySettingsChanges(final AppSettings oldSettings, final AppSettings newSettings) {
        final AppSettings.Diff diff = new AppSettings.Diff(oldSettings, newSettings);
        final IAppSettingsChangeListener l = SettingsManager.listeners.getListener();
        l.onAppSettingsChanged(oldSettings, newSettings, diff);
        return diff;
    }

    @Override
    public String key() {
        return BACKUP_KEY;
    }

    @Override
    public JSONObject backup() {
        return SettingsBackupHelper.backup(BACKUP_KEY, SettingsManager.prefs, AppPreferences.class);
    }

    @Override
    public void restore(final JSONObject backup) {
        SettingsBackupHelper.restore(BACKUP_KEY, SettingsManager.prefs, AppPreferences.class, backup);
        onSettingsChanged();
    }

    public static class Diff {

        private static final int D_Lang = 0x0001 << 0;
        private static final int D_Rotation = 0x0001 << 1;
        private static final int D_FullScreen = 0x0001 << 2;
        private static final int D_ShowTitle = 0x0001 << 3;
        private static final int D_PageInTitle = 0x0001 << 4;
        private static final int D_TapsEnabled = 0x0001 << 5;
        private static final int D_ScrollHeight = 0x0001 << 6;
        private static final int D_PagesInMemory = 0x0001 << 7;
        private static final int D_Brightness = 0x0001 << 8;
        private static final int D_BrightnessInNightMode = 0x0001 << 9;
        private static final int D_KeepScreenOn = 0x0001 << 10;
        private static final int D_LoadRecent = 0x0001 << 11;
        private static final int D_UseBookcase = 0x0001 << 12;
        private static final int D_DjvuRenderingMode = 0x0001 << 13;
        private static final int D_AutoScanDirs = 0x0001 << 14;
        private static final int D_AllowedFileTypes = 0x0001 << 15;
        private static final int D_TapConfigChanged = 0x0001 << 16;
        private static final int D_KeyBindingChanged = 0x0001 << 17;

        private int mask;
        private final boolean firstTime;

        public Diff(final AppSettings olds, final AppSettings news) {
            firstTime = olds == null;
            if (firstTime) {
                mask = 0xFFFFFFFF;
            } else if (news != null) {
                if (CompareUtils.compare(olds.lang, news.lang) != 0) {
                    mask |= D_Lang;
                }
                if (olds.rotation != news.rotation) {
                    mask |= D_Rotation;
                }
                if (olds.fullScreen != news.fullScreen) {
                    mask |= D_FullScreen;
                }
                if (olds.showTitle != news.showTitle) {
                    mask |= D_ShowTitle;
                }
                if (olds.pageInTitle != news.pageInTitle) {
                    mask |= D_PageInTitle;
                }
                if (olds.tapsEnabled != news.tapsEnabled) {
                    mask |= D_TapsEnabled;
                }
                if (olds.scrollHeight != news.scrollHeight) {
                    mask |= D_ScrollHeight;
                }
                if (olds.pagesInMemory != news.pagesInMemory) {
                    mask |= D_PagesInMemory;
                }
                if (olds.brightness != news.brightness) {
                    mask |= D_Brightness;
                }
                if (olds.brightnessInNightModeOnly != news.brightnessInNightModeOnly) {
                    mask |= D_BrightnessInNightMode;
                }
                if (olds.keepScreenOn != news.keepScreenOn) {
                    mask |= D_KeepScreenOn;
                }
                if (olds.loadRecent != news.loadRecent) {
                    mask |= D_LoadRecent;
                }
                if (olds.djvuRenderingMode != news.djvuRenderingMode) {
                    mask |= D_DjvuRenderingMode;
                }
                if (!olds.tapProfiles.equals(news.tapProfiles)) {
                    mask |= D_TapConfigChanged;
                }
                if (!olds.keysBinding.equals(news.keysBinding)) {
                    mask |= D_KeyBindingChanged;
                }
            }
        }

        public boolean isFirstTime() {
            return firstTime;
        }

        public boolean isLangChanged() {
            return 0 != (mask & D_Lang);
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

        public boolean isTapsEnabledChanged() {
            return 0 != (mask & D_TapsEnabled);
        }

        public boolean isScrollHeightChanged() {
            return 0 != (mask & D_ScrollHeight);
        }

        public boolean isPagesInMemoryChanged() {
            return 0 != (mask & D_PagesInMemory);
        }

        public boolean isBrightnessChanged() {
            return 0 != (mask & D_Brightness);
        }

        public boolean isBrightnessInNightModeChanged() {
            return 0 != (mask & D_BrightnessInNightMode);
        }

        public boolean isKeepScreenOnChanged() {
            return 0 != (mask & D_KeepScreenOn);
        }

        public boolean isLoadRecentChanged() {
            return 0 != (mask & D_LoadRecent);
        }

        public boolean isUseBookcaseChanged() {
            return 0 != (mask & D_UseBookcase);
        }

        public boolean isDjvuRenderingModeChanged() {
            return 0 != (mask & D_DjvuRenderingMode);
        }

        public boolean isAutoScanDirsChanged() {
            return 0 != (mask & D_AutoScanDirs);
        }

        public boolean isAllowedFileTypesChanged() {
            return 0 != (mask & D_AllowedFileTypes);
        }

        public boolean isTapConfigChanged() {
            return 0 != (mask & D_TapConfigChanged);
        }

        public boolean isKeyBindingChanged() {
            return 0 != (mask & D_KeyBindingChanged);
        }
    }
}
