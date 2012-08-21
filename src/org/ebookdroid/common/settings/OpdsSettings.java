package org.ebookdroid.common.settings;

import org.ebookdroid.common.settings.definitions.OpdsPreferences;
import org.ebookdroid.common.settings.listeners.IOpdsSettingsChangeListener;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.emdev.common.backup.BackupManager;
import org.emdev.common.backup.IBackupAgent;
import org.emdev.common.settings.backup.SettingsBackupHelper;
import org.json.JSONArray;
import org.json.JSONObject;

public class OpdsSettings implements OpdsPreferences, IBackupAgent {

    public static final String BACKUP_KEY = "opds-settings";

    private static OpdsSettings current;

    /* =============== OPDS settings =============== */

    public final JSONArray opdsCatalogs;
    public final String downloadDir;
    public final boolean filterTypes;
    public final boolean downloadArchives;
    public final boolean unpackArchives;
    public final boolean deleteArchives;

    private OpdsSettings() {
        BackupManager.addAgent(this);
        SharedPreferences prefs = SettingsManager.prefs;
        /* =============== OPDS settings =============== */
        opdsCatalogs = OPDS_CATALOGS.getPreferenceValue(prefs);
        downloadDir = OPDS_DOWNLOAD_DIR.getPreferenceValue(prefs);
        filterTypes = OPDS_FILTER_TYPES.getPreferenceValue(prefs);
        downloadArchives = OPDS_DOWNLOAD_ARCHIVES.getPreferenceValue(prefs);
        unpackArchives = OPDS_UNPACK_ARCHIVES.getPreferenceValue(prefs);
        deleteArchives = OPDS_DELETE_ARCHIVES.getPreferenceValue(prefs);
    }

    /* =============== OPDS settings =============== */

    /* =============== */

    public static void init() {
        current = new OpdsSettings();
    }

    public static OpdsSettings current() {
        SettingsManager.lock.readLock().lock();
        try {
            return current;
        } finally {
            SettingsManager.lock.readLock().unlock();
        }
    }

    public static void changeOpdsCatalogs(final JSONArray opdsCatalogs) {
        SettingsManager.lock.writeLock().lock();
        try {
            final Editor edit = SettingsManager.prefs.edit();
            OpdsPreferences.OPDS_CATALOGS.setPreferenceValue(edit, opdsCatalogs);
            edit.commit();
            final OpdsSettings oldSettings = current;
            current = new OpdsSettings();
            applySettingsChanges(oldSettings, current);
        } finally {
            SettingsManager.lock.writeLock().unlock();
        }
    }

    static Diff onSettingsChanged() {
        final OpdsSettings oldOpdsSettings = current;
        current = new OpdsSettings();
        return applySettingsChanges(oldOpdsSettings, current);
    }

    public static OpdsSettings.Diff applySettingsChanges(final OpdsSettings oldSettings,
            final OpdsSettings newSettings) {
        final OpdsSettings.Diff diff = new OpdsSettings.Diff(oldSettings, newSettings);
        final IOpdsSettingsChangeListener l = SettingsManager.listeners.getListener();
        l.onOpdsSettingsChanged(oldSettings, newSettings, diff);
        return diff;
    }

    @Override
    public String key() {
        return BACKUP_KEY;
    }

    @Override
    public JSONObject backup() {
        return SettingsBackupHelper.backup(BACKUP_KEY, SettingsManager.prefs, OpdsPreferences.class);
    }

    @Override
    public void restore(final JSONObject backup) {
        SettingsBackupHelper.restore(BACKUP_KEY, SettingsManager.prefs, OpdsPreferences.class, backup);
        onSettingsChanged();
    }

    public static class Diff {

        private static final int D_OpdsCatalogs = 0x0001 << 0;

        private int mask;
        private final boolean firstTime;

        public Diff(final OpdsSettings olds, final OpdsSettings news) {
            firstTime = olds == null;
            if (firstTime) {
                mask = 0xFFFFFFFF;
            } else if (news != null) {
                if (!olds.opdsCatalogs.equals(news.opdsCatalogs)) {
                    mask |= D_OpdsCatalogs;
                }
            }
        }

        public boolean isFirstTime() {
            return firstTime;
        }

        public boolean isOpdsCatalogsChanged() {
            return 0 != (mask & D_OpdsCatalogs);
        }
    }
}
