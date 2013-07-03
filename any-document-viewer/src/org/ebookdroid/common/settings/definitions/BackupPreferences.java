package org.ebookdroid.common.settings.definitions;

import static org.ebookdroid.R.string.*;

import org.ebookdroid.common.settings.books.BookBackupType;

import org.emdev.common.settings.base.BooleanPreferenceDefinition;
import org.emdev.common.settings.base.EnumPreferenceDefinition;
import org.emdev.common.settings.base.IntegerPreferenceDefinition;

public interface BackupPreferences {

    /* =============== Backup settings =============== */

    BooleanPreferenceDefinition BACKUP_ON_EXIT = new BooleanPreferenceDefinition(pref_backuponexit_id,
            pref_backuponexit_defvalue);

    BooleanPreferenceDefinition BACKUP_ON_BOOK_CLOSE = new BooleanPreferenceDefinition(pref_backuponbookclose_id,
            pref_backuponbookclose_defvalue);

    IntegerPreferenceDefinition MAX_NUMBER_OF_AUTO_BACKUPS = new IntegerPreferenceDefinition(
            pref_maxnumberofautobackups_id, pref_maxnumberofautobackups_defvalue, pref_maxnumberofautobackups_minvalue,
            pref_maxnumberofautobackups_maxvalue);

    EnumPreferenceDefinition<BookBackupType> BOOK_BACKUP_TYPE = new EnumPreferenceDefinition<BookBackupType>(
            BookBackupType.class, pref_bookbackuptype_id, pref_bookbackuptype_defvalue);
}
