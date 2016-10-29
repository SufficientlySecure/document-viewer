package org.ebookdroid.common.settings.books;

import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.definitions.AppPreferences;
import org.ebookdroid.common.settings.definitions.BookPreferences;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.common.settings.types.RotationType;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.curl.PageAnimationType;
import org.ebookdroid.core.events.CurrentPageListener;

import java.util.ArrayList;
import java.util.List;

import org.emdev.utils.LengthUtils;
import org.emdev.utils.enums.EnumUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BookSettings implements CurrentPageListener {

    public transient boolean persistent;

    public transient long lastChanged;

    public final String fileName;

    public long lastUpdated;

    public int firstPageOffset = 1;

    public PageIndex currentPage;

    public int zoom = 100;

    public boolean splitPages;

    public boolean splitRTL;

    public RotationType rotation;

    public DocumentViewMode viewMode;

    public PageAlign pageAlign = PageAlign.AUTO;

    public PageAnimationType animationType = PageAnimationType.NONE;

    public final List<Bookmark> bookmarks = new ArrayList<Bookmark>();

    public boolean cropPages;

    public float offsetX;

    public float offsetY;

    public boolean nightMode;

    public boolean positiveImagesInNightMode;

    public boolean tint;

    public int tintColor;

    public int contrast = AppPreferences.CONTRAST.defValue;

    public int gamma = AppPreferences.GAMMA.defValue;

    public int exposure = AppPreferences.EXPOSURE.defValue;

    public boolean autoLevels;

    public boolean rtl;

    public JSONObject typeSpecific;

    public BookSettings(final BookSettings current) {
        this.persistent = current.persistent;
        this.lastChanged = current.lastChanged;
        this.fileName = current.fileName;
        this.lastUpdated = current.lastUpdated;

        this.firstPageOffset = current.firstPageOffset;
        this.currentPage = current.currentPage;
        this.zoom = current.zoom;
        this.splitPages = current.splitPages;
        this.splitRTL = current.splitRTL;
        this.rotation = current.rotation;
        this.viewMode = current.viewMode;
        this.pageAlign = current.pageAlign;
        this.animationType = current.animationType;
        this.bookmarks.addAll(current.bookmarks);
        this.cropPages = current.cropPages;
        this.offsetX = current.offsetX;
        this.offsetY = current.offsetY;
        this.nightMode = current.nightMode;
        this.positiveImagesInNightMode = current.positiveImagesInNightMode;
        this.tint = current.tint;
        this.tintColor = current.tintColor;
        this.contrast = current.contrast;
        this.gamma = current.gamma;
        this.exposure = current.exposure;
        this.autoLevels = current.autoLevels;
        this.rtl = current.rtl;
        try {
            this.typeSpecific = current.typeSpecific != null ? new JSONObject(current.typeSpecific.toString()) : null;
        } catch (JSONException e) {
        }
    }

    public BookSettings(final String fileName, final BookSettings current) {
        this.persistent = true;
        this.lastChanged = 0;
        this.fileName = fileName;
        this.lastUpdated = current.lastUpdated;

        this.firstPageOffset = current.firstPageOffset;
        this.currentPage = current.currentPage;
        this.zoom = current.zoom;
        this.splitPages = current.splitPages;
        this.splitRTL = current.splitRTL;
        this.rotation = current.rotation;
        this.viewMode = current.viewMode;
        this.pageAlign = current.pageAlign;
        this.animationType = current.animationType;
        this.bookmarks.addAll(current.bookmarks);
        this.cropPages = current.cropPages;
        this.offsetX = current.offsetX;
        this.offsetY = current.offsetY;
        this.nightMode = current.nightMode;
        this.positiveImagesInNightMode = current.positiveImagesInNightMode;
        this.tint = current.tint;
        this.tintColor = current.tintColor;
        this.contrast = current.contrast;
        this.gamma = current.gamma;
        this.exposure = current.exposure;
        this.autoLevels = current.autoLevels;
        this.rtl = current.rtl;
        try {
            this.typeSpecific = current.typeSpecific != null ? new JSONObject(current.typeSpecific.toString()) : null;
        } catch (JSONException e) {
        }
    }

    public BookSettings(final String fileName) {
        this.persistent = true;
        this.lastChanged = 0;
        this.fileName = fileName;
        this.lastUpdated = System.currentTimeMillis();
        this.currentPage = PageIndex.FIRST;
    }

    BookSettings(final JSONObject object) throws JSONException {
        this.persistent = true;
        this.lastChanged = 0;
        this.fileName = object.getString("fileName");
        this.lastUpdated = object.getLong("lastUpdated");

        this.firstPageOffset = object.optInt("firstPageOffset", 1);
        this.currentPage = new PageIndex(object.getJSONObject("currentPage"));
        this.zoom = object.getInt("zoom");
        this.splitPages = object.getBoolean("splitPages");
        this.splitRTL = object.optBoolean("splitRTL", false);
        this.rotation = EnumUtils.getByName(RotationType.class, object, "rotation", RotationType.UNSPECIFIED);
        this.viewMode = EnumUtils.getByName(DocumentViewMode.class, object, "viewMode", DocumentViewMode.VERTICALL_SCROLL);
        this.pageAlign = EnumUtils.getByName(PageAlign.class, object, "pageAlign", PageAlign.AUTO);
        this.animationType = EnumUtils.getByName(PageAnimationType.class, object, "animationType", PageAnimationType.NONE);
        this.cropPages = object.getBoolean("cropPages");
        this.offsetX = (float) object.getDouble("offsetX");
        this.offsetY = (float) object.getDouble("offsetY");
        this.nightMode = object.getBoolean("nightMode");
        this.positiveImagesInNightMode = object.optBoolean("positiveImagesInNightMode", false);
        this.tint = object.optBoolean("tint", false);
        this.tintColor = object.optInt("tintColor", 0);
        this.contrast = object.getInt("contrast");
        this.gamma = object.optInt("gamma", AppPreferences.GAMMA.defValue);
        this.exposure = object.getInt("exposure");
        this.autoLevels = object.getBoolean("autoLevels");
        this.rtl = object.optBoolean("rtl", BookPreferences.BOOK_RTL.getDefaultValue());

        final JSONArray bookmarks = object.optJSONArray("bookmarks");
        if (LengthUtils.isNotEmpty(bookmarks)) {
            for (int i = 0, n = bookmarks.length(); i < n; i++) {
                final JSONObject obj = bookmarks.getJSONObject(i);
                this.bookmarks.add(new Bookmark(obj));
            }
        }

        this.typeSpecific = object.optJSONObject("typeSpecific");

    }

    JSONObject toJSON() throws JSONException {
        final JSONObject obj = new JSONObject();
        obj.put("fileName", fileName);
        obj.put("lastUpdated", lastUpdated);
        obj.put("firstPageOffset", firstPageOffset);
        obj.put("currentPage", currentPage != null ? currentPage.toJSON() : null);
        obj.put("zoom", zoom);
        obj.put("splitPages", splitPages);
        obj.put("splitRTL", splitRTL);
        obj.put("rotation", rotation != null ? rotation.name() : null);
        obj.put("viewMode", viewMode != null ? viewMode.name() : null);
        obj.put("pageAlign", pageAlign != null ? pageAlign.name() : null);
        obj.put("animationType", animationType != null ? animationType.name() : null);
        obj.put("cropPages", cropPages);
        obj.put("offsetX", offsetX);
        obj.put("offsetY", offsetY);
        obj.put("nightMode", nightMode);
        obj.put("positiveImagesInNightMode", positiveImagesInNightMode);
        obj.put("tint", tint);
        obj.put("tintColor", tintColor);
        obj.put("contrast", contrast);
        obj.put("gamma", gamma);
        obj.put("exposure", exposure);
        obj.put("autoLevels", autoLevels);
        obj.put("rtl", rtl);

        final JSONArray bookmarks = new JSONArray();
        obj.put("bookmarks", bookmarks);
        for (final Bookmark b : this.bookmarks) {
            bookmarks.put(b.toJSON());
        }

        obj.putOpt("typeSpecific", typeSpecific);

        return obj;
    }

    @Override
    public void currentPageChanged(final PageIndex oldIndex, final PageIndex newIndex) {
        this.currentPage = newIndex;
        this.lastChanged = System.currentTimeMillis();
    }

    public void positionChanged(final float offsetX, final float offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.lastChanged = System.currentTimeMillis();
    }

    public PageIndex getCurrentPage() {
        return currentPage;
    }

    public float getZoom() {
        return zoom / 100.0f;
    }

    public void setZoom(final float zoom, final boolean committed) {
        this.zoom = Math.round(zoom * 100);
        if (committed) {
            this.lastChanged = System.currentTimeMillis();
        }
    }

    public int getOrientation(final AppSettings appSettings) {
        if (rotation == null || rotation == RotationType.UNSPECIFIED) {
            final RotationType defRotation = appSettings.rotation;
            return defRotation.getOrientation();
        }
        return rotation.getOrientation();
    }

    public static class Diff {

        private static final int D_SplitPages = 0x0001 << 2;
        private static final int D_ViewMode = 0x0001 << 3;
        private static final int D_PageAlign = 0x0001 << 4;
        private static final int D_AnimationType = 0x0001 << 5;
        private static final int D_CropPages = 0x0001 << 6;
        private static final int D_Contrast = 0x0001 << 7;
        private static final int D_Exposure = 0x0001 << 8;
        private static final int D_NightMode = 0x0001 << 9;
        private static final int D_AutoLevels = 0x0001 << 10;
        private static final int D_PositiveImagesInNightMode = 0x0001 << 11;
        private static final int D_Rotation = 0x0001 << 12;
        private static final int D_Gamma = 0x0001 << 13;
        private static final int D_Tint = 0x0001 << 14;
        private static final int D_RTL = 0x0001 << 15;

        private static final int D_Effects = D_Contrast | D_Exposure | D_NightMode | D_PositiveImagesInNightMode
                | D_AutoLevels | D_Gamma | D_Tint;

        private int mask;
        private final boolean firstTime;

        public Diff(final BookSettings olds, final BookSettings news) {
            firstTime = olds == null;
            if (firstTime) {
                mask = 0xFFFFFFFF;
            } else if (news != null) {
                if (olds.splitPages != news.splitPages || olds.splitRTL != news.splitRTL) {
                    mask |= D_SplitPages;
                }
                if (olds.cropPages != news.cropPages) {
                    mask |= D_CropPages;
                }
                if (olds.rotation != news.rotation) {
                    mask |= D_Rotation;
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
                if (olds.gamma != news.gamma) {
                    mask |= D_Gamma;
                }
                if (olds.exposure != news.exposure) {
                    mask |= D_Exposure;
                }
                if (olds.nightMode != news.nightMode) {
                    mask |= D_NightMode;
                }
                if (olds.positiveImagesInNightMode != news.positiveImagesInNightMode) {
                    mask |= D_PositiveImagesInNightMode;
                }
                if (olds.tint != news.tint) {
                    mask |= D_Tint;
                }
                if (olds.tintColor != news.tintColor) {
                    mask |= D_Tint;
                }
                if (olds.autoLevels != news.autoLevels) {
                    mask |= D_AutoLevels;
                }
                if (olds.rtl != news.rtl) {
                    mask |= D_RTL;
                }
            }
        }

        public boolean isFirstTime() {
            return firstTime;
        }

        public boolean isSplitPagesChanged() {
            return 0 != (mask & D_SplitPages);
        }

        public boolean isRotationChanged() {
            return 0 != (mask & D_Rotation);
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

        public boolean isGammaChanged() {
            return 0 != (mask & D_Gamma);
        }

        public boolean isExposureChanged() {
            return 0 != (mask & D_Exposure);
        }

        public boolean isNightModeChanged() {
            return 0 != (mask & D_NightMode);
        }

        public boolean isPositiveImagesInNightModeChanged() {
            return 0 != (mask & D_PositiveImagesInNightMode);
        }

        public boolean isAutoLevelsChanged() {
            return 0 != (mask & D_AutoLevels);
        }

        public boolean isEffectsChanged() {
            return 0 != (mask & (D_Effects));
        }

        public boolean isRTLChanged() {
            return 0 != (mask & (D_RTL));
        }
    }
}
