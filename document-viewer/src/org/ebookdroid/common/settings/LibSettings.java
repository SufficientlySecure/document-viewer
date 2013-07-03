package org.ebookdroid.common.settings;

import org.ebookdroid.common.settings.definitions.LibPreferences;
import org.ebookdroid.common.settings.listeners.ILibSettingsChangeListener;
import org.ebookdroid.common.settings.types.CacheLocation;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.util.HashSet;
import java.util.Set;

import org.emdev.common.backup.BackupManager;
import org.emdev.common.backup.IBackupAgent;
import org.emdev.common.filesystem.FileExtensionFilter;
import org.emdev.common.settings.backup.SettingsBackupHelper;
import org.json.JSONObject;

public class LibSettings implements LibPreferences, IBackupAgent {

    public static final String BACKUP_KEY = "lib-settings";

    private static LibSettings current;

    /* =============== Browser settings =============== */

    public final boolean useBookcase;

    public final Set<String> autoScanDirs;

    public final String searchBookQuery;

    public final FileExtensionFilter allowedFileTypes;

    public final CacheLocation cacheLocation;

    public final boolean autoScanRemovableMedia;

    public final boolean showScanningInMenu;

    public final boolean showRemovableMediaInMenu;

    public final boolean showNotifications;

    private LibSettings() {
        BackupManager.addAgent(this);
        final SharedPreferences prefs = SettingsManager.prefs;
        /* =============== Browser settings =============== */
        useBookcase = USE_BOOK_CASE.getPreferenceValue(prefs);
        autoScanDirs = AUTO_SCAN_DIRS.getPreferenceValue(prefs);
        searchBookQuery = SEARCH_BOOK_QUERY.getPreferenceValue(prefs);
        allowedFileTypes = FILE_TYPE_FILTER.getFilter(prefs);
        cacheLocation  = CACHE_LOCATION.getPreferenceValue(prefs);
        autoScanRemovableMedia = AUTO_SCAN_REMOVABLE_MEDIA.getPreferenceValue(prefs);
        showRemovableMediaInMenu = SHOW_REMOVABLE_MEDIA.getPreferenceValue(prefs);
        showScanningInMenu = SHOW_SCANNING_MEDIA.getPreferenceValue(prefs);
        showNotifications = SHOW_NOTIFICATIONS.getPreferenceValue(prefs);
    }

    /* =============== */

    public static void init() {
        current = new LibSettings();
    }

    public static LibSettings current() {
        SettingsManager.lock.readLock().lock();
        try {
            return current;
        } finally {
            SettingsManager.lock.readLock().unlock();
        }
    }

    public static void changeAutoScanDirs(final String dir, final boolean add) {
        SettingsManager.lock.writeLock().lock();
        try {
            final Set<String> dirs = new HashSet<String>(current.autoScanDirs);
            if (add && dirs.add(dir) || dirs.remove(dir)) {
                final Editor edit = SettingsManager.prefs.edit();
                LibPreferences.AUTO_SCAN_DIRS.setPreferenceValue(edit, dirs);
                edit.commit();
                final LibSettings oldSettings = current;
                current = new LibSettings();
                applySettingsChanges(oldSettings, current);
            }
        } finally {
            SettingsManager.lock.writeLock().unlock();
        }
    }

    public static void updateSearchBookQuery(final String searchQuery) {
        SettingsManager.lock.writeLock().lock();
        try {
            final Editor edit = SettingsManager.prefs.edit();
            LibPreferences.SEARCH_BOOK_QUERY.setPreferenceValue(edit, searchQuery);
            edit.commit();
            final LibSettings oldSettings = current;
            current = new LibSettings();
            applySettingsChanges(oldSettings, current);
        } finally {
            SettingsManager.lock.writeLock().unlock();
        }
    }

    static Diff onSettingsChanged() {
        final LibSettings oldLibSettings = current;
        current = new LibSettings();
        return applySettingsChanges(oldLibSettings, current);
    }

    public static LibSettings.Diff applySettingsChanges(final LibSettings oldSettings, final LibSettings newSettings) {
        final LibSettings.Diff diff = new LibSettings.Diff(oldSettings, newSettings);
        final ILibSettingsChangeListener l = SettingsManager.listeners.getListener();
        l.onLibSettingsChanged(oldSettings, newSettings, diff);
        return diff;
    }

    @Override
    public String key() {
        return BACKUP_KEY;
    }

    @Override
    public JSONObject backup() {
        return SettingsBackupHelper.backup(BACKUP_KEY, SettingsManager.prefs, LibPreferences.class);
    }

    @Override
    public void restore(final JSONObject backup) {
        SettingsBackupHelper.restore(BACKUP_KEY, SettingsManager.prefs, LibPreferences.class, backup);
        onSettingsChanged();
    }

    public static class Diff {

        private static final int D_UseBookcase = 0x0001 << 0;
        private static final int D_AutoScanDirs = 0x0001 << 1;
        private static final int D_AllowedFileTypes = 0x0001 << 2;
        private static final int D_CacheLocation = 0x0001 << 3;
        private static final int D_AutoScanMedia = 0x0001 << 4;

        private int mask;
        private final boolean firstTime;

        public Diff(final LibSettings olds, final LibSettings news) {
            firstTime = olds == null;
            if (firstTime) {
                mask = 0xFFFFFFFF;
            } else if (news != null) {
                if (olds.useBookcase != news.useBookcase) {
                    mask |= D_UseBookcase;
                }
                if (!olds.autoScanDirs.equals(news.autoScanDirs)) {
                    mask |= D_AutoScanDirs;
                }
                if (!olds.allowedFileTypes.equals(news.allowedFileTypes)) {
                    mask |= D_AllowedFileTypes;
                }
                if (olds.cacheLocation != news.cacheLocation) {
                    mask |= D_CacheLocation;
                }
                if (olds.autoScanRemovableMedia != news.autoScanRemovableMedia) {
                    mask |= D_AutoScanMedia;
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

        public boolean isCacheLocationChanged() {
            return 0 != (mask & D_CacheLocation);
        }

        public boolean isAutoScanRemovableMediaChanged() {
            return 0 != (mask & D_AutoScanMedia);
        }
    }
}
