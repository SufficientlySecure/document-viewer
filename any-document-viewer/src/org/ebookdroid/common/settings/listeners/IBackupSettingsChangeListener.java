package org.ebookdroid.common.settings.listeners;

import org.ebookdroid.common.settings.BackupSettings;

public interface IBackupSettingsChangeListener {

    void onBackupSettingsChanged(BackupSettings oldSettings, BackupSettings newSettings, BackupSettings.Diff diff);

}
