package org.ebookdroid.common.settings;

import org.ebookdroid.common.settings.books.BookBackupType;
import org.ebookdroid.common.settings.definitions.BackupPreferences;
import org.ebookdroid.common.settings.listeners.IBackupSettingsChangeListener;

import android.content.SharedPreferences;

import org.emdev.common.backup.BackupManager;
import org.emdev.common.backup.IBackupAgent;
import org.emdev.common.settings.backup.SettingsBackupHelper;
import org.json.JSONObject;

public class BackupSettings implements BackupPreferences, IBackupAgent {

    public static final String BACKUP_KEY = "backup-settings";

    private static BackupSettings current;

    /* =============== Backup settings =============== */

    public final boolean backupOnExit;

    public final boolean backupOnBookClose;

    public final int maxNumberOfAutoBackups;

    public final BookBackupType bookBackup;

    private BackupSettings() {
        BackupManager.addAgent(this);
        final SharedPreferences prefs = SettingsManager.prefs;
        /* =============== Backup settings =============== */
        backupOnExit = BACKUP_ON_EXIT.getPreferenceValue(prefs);
        backupOnBookClose = BACKUP_ON_BOOK_CLOSE.getPreferenceValue(prefs);
        maxNumberOfAutoBackups = MAX_NUMBER_OF_AUTO_BACKUPS.getPreferenceValue(prefs);
        bookBackup = BOOK_BACKUP_TYPE.getPreferenceValue(prefs);
    }

    public static void init() {
        current = new BackupSettings();
    }

    public static BackupSettings current() {
        SettingsManager.lock.readLock().lock();
        try {
            return current;
        } finally {
            SettingsManager.lock.readLock().unlock();
        }
    }

    @Override
    public String key() {
        return BACKUP_KEY;
    }

    @Override
    public JSONObject backup() {
        return SettingsBackupHelper.backup(BACKUP_KEY, SettingsManager.prefs, BackupPreferences.class);
    }

    @Override
    public void restore(final JSONObject backup) {
        // For backup compatibility
        SettingsBackupHelper.restore(AppSettings.BACKUP_KEY, SettingsManager.prefs, BackupPreferences.class, backup);

        SettingsBackupHelper.restore(BACKUP_KEY, SettingsManager.prefs, BackupPreferences.class, backup);
        onSettingsChanged();
    }

    static Diff onSettingsChanged() {
        final BackupSettings oldAppSettings = current;
        current = new BackupSettings();
        return applySettingsChanges(oldAppSettings, current);
    }

    static BackupSettings.Diff applySettingsChanges(final BackupSettings oldSettings, final BackupSettings newSettings) {
        final BackupSettings.Diff diff = new BackupSettings.Diff(oldSettings, newSettings);
        final IBackupSettingsChangeListener l = SettingsManager.listeners.getListener();
        l.onBackupSettingsChanged(oldSettings, newSettings, diff);
        return diff;
    }

    public static class Diff {

        public Diff(final BackupSettings oldSettings, final BackupSettings newSettings) {
            // TODO Auto-generated constructor stub
        }
    }
}
