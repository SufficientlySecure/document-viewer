package org.ebookdroid.common.settings.definitions;

import org.sufficientlysecure.viewer.R;

import org.ebookdroid.common.settings.books.BookBackupType;

import org.emdev.common.settings.base.BooleanPreferenceDefinition;
import org.emdev.common.settings.base.EnumPreferenceDefinition;
import org.emdev.common.settings.base.IntegerPreferenceDefinition;

public interface BackupPreferences {

    /* =============== Backup settings =============== */

    BooleanPreferenceDefinition BACKUP_ON_EXIT = new BooleanPreferenceDefinition(R.string.pref_backuponexit_id,
            R.string.pref_backuponexit_defvalue);

    BooleanPreferenceDefinition BACKUP_ON_BOOK_CLOSE = new BooleanPreferenceDefinition(R.string.pref_backuponbookclose_id,
            R.string.pref_backuponbookclose_defvalue);

    IntegerPreferenceDefinition MAX_NUMBER_OF_AUTO_BACKUPS = new IntegerPreferenceDefinition(
            R.string.pref_maxnumberofautobackups_id, R.string.pref_maxnumberofautobackups_defvalue, R.string.pref_maxnumberofautobackups_minvalue,
            R.string.pref_maxnumberofautobackups_maxvalue);

    EnumPreferenceDefinition<BookBackupType> BOOK_BACKUP_TYPE = new EnumPreferenceDefinition<BookBackupType>(
            BookBackupType.class, R.string.pref_bookbackuptype_id, R.string.pref_bookbackuptype_defvalue);
}
