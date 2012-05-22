package org.ebookdroid.common.settings;

import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.definitions.AppPreferences;
import org.ebookdroid.common.settings.definitions.BookPreferences;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.settings.types.DocumentViewType;
import org.ebookdroid.common.settings.types.FontSize;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.common.settings.types.RotationType;
import org.ebookdroid.common.settings.types.ToastPosition;
import org.ebookdroid.core.curl.PageAnimationType;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class AppSettings implements AppPreferences, BookPreferences {

    private final SharedPreferences prefs;

    /* =============== UI settings =============== */

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

    public final int touchProcessingDelay;

    public final boolean animateScrolling;

    /* =============== Tap & Keyboard settings =============== */

    public final String tapProfiles;

    public final String keysBinding;

    /* =============== Performance settings =============== */

    public final int pagesInMemory;

    public final DocumentViewType viewType;

    public final int decodingThreadPriority;

    public final int drawThreadPriority;

    public final boolean useNativeGraphics;

    public final boolean hwaEnabled;

    public final int bitmapSize;

    public final boolean bitmapFileringEnabled;

    public final boolean textureReuseEnabled;

    public final boolean useBitmapHack;

    public final boolean useEarlyRecycling;

    public final boolean reloadDuringZoom;

    /* =============== Default rendering settings =============== */

    public final boolean nightMode;

    final int contrast;

    final int exposure;

    final boolean autoLevels;

    final boolean splitPages;

    final boolean cropPages;

    public final DocumentViewMode viewMode;

    final PageAlign pageAlign;

    final PageAnimationType animationType;

    /* =============== Format-specific settings =============== */

    public final int djvuRenderingMode;

    public final boolean useCustomDpi;

    public final int xDpi;

    public final int yDpi;

    public final FontSize fontSize;

    public final boolean fb2HyphenEnabled;

    AppSettings(final SharedPreferences prefs) {
        this.prefs = prefs;
        /* =============== UI settings =============== */
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
        touchProcessingDelay = TOUCH_DELAY.getPreferenceValue(prefs);
        animateScrolling = ANIMATE_SCROLLING.getPreferenceValue(prefs);
        /* =============== Tap & Keyboard settings =============== */
        tapProfiles = TAP_PROFILES.getPreferenceValue(prefs);
        keysBinding = KEY_BINDINGS.getPreferenceValue(prefs);
        /* =============== Performance settings =============== */
        pagesInMemory = PAGES_IN_MEMORY.getPreferenceValue(prefs);
        viewType = VIEW_TYPE.getPreferenceValue(prefs);
        decodingThreadPriority = DECODE_THREAD_PRIORITY.getPreferenceValue(prefs);
        drawThreadPriority = DRAW_THREAD_PRIORITY.getPreferenceValue(prefs);
        useNativeGraphics = USE_NATIVE_GRAPHICS.getPreferenceValue(prefs);
        hwaEnabled = HWA_ENABLED.getPreferenceValue(prefs);
        bitmapSize = BITMAP_SIZE.getPreferenceValue(prefs);
        bitmapFileringEnabled = BITMAP_FILTERING.getPreferenceValue(prefs);
        textureReuseEnabled = REUSE_TEXTURES.getPreferenceValue(prefs);
        useBitmapHack = USE_BITMAP_HACK.getPreferenceValue(prefs);
        useEarlyRecycling = EARLY_RECYCLING.getPreferenceValue(prefs);
        reloadDuringZoom = RELOAD_DURING_ZOOM.getPreferenceValue(prefs);
        /* =============== Default rendering settings =============== */
        nightMode = NIGHT_MODE.getPreferenceValue(prefs);
        contrast = CONTRAST.getPreferenceValue(prefs);
        exposure = EXPOSURE.getPreferenceValue(prefs);
        autoLevels = AUTO_LEVELS.getPreferenceValue(prefs);
        splitPages = SPLIT_PAGES.getPreferenceValue(prefs);
        cropPages = CROP_PAGES.getPreferenceValue(prefs);
        viewMode = VIEW_MODE.getPreferenceValue(prefs);
        pageAlign = PAGE_ALIGN.getPreferenceValue(prefs);
        animationType = ANIMATION_TYPE.getPreferenceValue(prefs);
        /* =============== Format-specific settings =============== */
        djvuRenderingMode = DJVU_RENDERING_MODE.getPreferenceValue(prefs);
        useCustomDpi = PDF_CUSTOM_DPI.getPreferenceValue(prefs);
        xDpi = PDF_CUSTOM_XDPI.getPreferenceValue(prefs);
        yDpi = PDF_CUSTOM_YDPI.getPreferenceValue(prefs);
        fontSize = FB2_FONT_SIZE.getPreferenceValue(prefs);
        fb2HyphenEnabled = FB2_HYPHEN.getPreferenceValue(prefs);
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

    void clearPseudoBookSettings() {
        final Editor edit = prefs.edit();
        edit.remove(BOOK.key);
        edit.remove(BOOK_NIGHT_MODE.key);
        edit.remove(BOOK_CONTRAST.key);
        edit.remove(BOOK_EXPOSURE.key);
        edit.remove(BOOK_AUTO_LEVELS.key);
        edit.remove(BOOK_SPLIT_PAGES.key);
        edit.remove(BOOK_CROP_PAGES.key);
        edit.remove(BOOK_VIEW_MODE.key);
        edit.remove(BOOK_PAGE_ALIGN.key);
        edit.remove(BOOK_ANIMATION_TYPE.key);
        edit.commit();
    }

    void updatePseudoBookSettings(final BookSettings bs) {
        final Editor edit = prefs.edit();
        BOOK.setPreferenceValue(edit, bs.fileName);
        BOOK_NIGHT_MODE.setPreferenceValue(edit, bs.nightMode);
        BOOK_CONTRAST.setPreferenceValue(edit, bs.contrast);
        BOOK_EXPOSURE.setPreferenceValue(edit, bs.exposure);
        BOOK_AUTO_LEVELS.setPreferenceValue(edit, bs.autoLevels);
        BOOK_SPLIT_PAGES.setPreferenceValue(edit, bs.splitPages);
        BOOK_CROP_PAGES.setPreferenceValue(edit, bs.cropPages);
        BOOK_VIEW_MODE.setPreferenceValue(edit, bs.viewMode);
        BOOK_PAGE_ALIGN.setPreferenceValue(edit, bs.pageAlign);
        BOOK_ANIMATION_TYPE.setPreferenceValue(edit, bs.animationType);
        edit.commit();
    }

    void fillBookSettings(final BookSettings bs) {
        bs.nightMode = BOOK_NIGHT_MODE.getPreferenceValue(prefs, nightMode);
        bs.contrast = BOOK_CONTRAST.getPreferenceValue(prefs, contrast);
        bs.exposure = BOOK_EXPOSURE.getPreferenceValue(prefs, exposure);
        bs.autoLevels = BOOK_AUTO_LEVELS.getPreferenceValue(prefs, autoLevels);
        bs.splitPages = BOOK_SPLIT_PAGES.getPreferenceValue(prefs, splitPages);
        bs.cropPages = BOOK_CROP_PAGES.getPreferenceValue(prefs, cropPages);
        bs.viewMode = BOOK_VIEW_MODE.getPreferenceValue(prefs, viewMode);
        bs.pageAlign = BOOK_PAGE_ALIGN.getPreferenceValue(prefs, pageAlign);
        bs.animationType = BOOK_ANIMATION_TYPE.getPreferenceValue(prefs, animationType);
    }

    public static class Diff {

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
