package org.ebookdroid.common.settings;

import org.ebookdroid.common.settings.definitions.LibPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Set;

import org.emdev.utils.android.AndroidVersion;
import org.emdev.utils.filesystem.FileExtensionFilter;

public class LibSettings implements LibPreferences {

    final SharedPreferences prefs;

    /* =============== Browser settings =============== */

    public final boolean useBookcase;

    public final Set<String> autoScanDirs;

    public final String searchBookQuery;

    public final FileExtensionFilter allowedFileTypes;

    LibSettings(final Context context) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        /* =============== Browser settings =============== */
        useBookcase = USE_BOOK_CASE.getPreferenceValue(prefs);
        autoScanDirs = AUTO_SCAN_DIRS.getPreferenceValue(prefs);
        searchBookQuery = SEARCH_BOOK_QUERY.getPreferenceValue(prefs);
        allowedFileTypes = FILE_TYPE_FILTER.getPreferenceValue(prefs);
    }

    /* =============== Browser settings =============== */

    public boolean getUseBookcase() {
        return !AndroidVersion.is1x && useBookcase;
    }

    /* =============== */

    public static class Diff {
        private static final int D_UseBookcase = 0x0001 << 12;
        private static final int D_AutoScanDirs = 0x0001 << 14;
        private static final int D_AllowedFileTypes = 0x0001 << 15;

        private int mask;
        private final boolean firstTime;

        public Diff(final LibSettings olds, final LibSettings news) {
            firstTime = olds == null;
            if (firstTime) {
                mask = 0xFFFFFFFF;
            } else if (news != null) {
                if (olds.getUseBookcase() != news.getUseBookcase()) {
                    mask |= D_UseBookcase;
                }
                if (!olds.autoScanDirs.equals(news.autoScanDirs)) {
                    mask |= D_AutoScanDirs;
                }
                if (!olds.allowedFileTypes.equals(news.allowedFileTypes)) {
                    mask |= D_AllowedFileTypes;
                }
            }
        }

        public boolean isFirstTime() {
            return firstTime;
        }

        public boolean isUseBookcaseChanged() {
            return 0 != (mask & D_UseBookcase);
        }

        public boolean isAutoScanDirsChanged() {
            return 0 != (mask & D_AutoScanDirs);
        }

        public boolean isAllowedFileTypesChanged() {
            return 0 != (mask & D_AllowedFileTypes);
        }
    }
}
