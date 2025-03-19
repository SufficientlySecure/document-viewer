package org.ebookdroid.common.settings.definitions;

import org.sufficientlysecure.viewer.R;

import org.ebookdroid.common.settings.base.FileTypeFilterPreferenceDefinition;
import org.ebookdroid.common.settings.types.CacheLocation;

import org.emdev.common.settings.base.BooleanPreferenceDefinition;
import org.emdev.common.settings.base.EnumPreferenceDefinition;
import org.emdev.common.settings.base.FileListPreferenceDefinition;
import org.emdev.common.settings.base.StringPreferenceDefinition;

public interface LibPreferences {

    /* =============== Browser settings =============== */

    BooleanPreferenceDefinition USE_BOOK_CASE = new BooleanPreferenceDefinition(R.string.pref_usebookcase_id,
            R.string.pref_usebookcase_defvalue);

    FileListPreferenceDefinition AUTO_SCAN_DIRS = new FileListPreferenceDefinition(R.string.pref_brautoscandir_id,
            R.string.pref_brautoscandir_defvalue);

    StringPreferenceDefinition SEARCH_BOOK_QUERY = new StringPreferenceDefinition(R.string.pref_brsearchbookquery_id, 0);

    FileTypeFilterPreferenceDefinition FILE_TYPE_FILTER = new FileTypeFilterPreferenceDefinition(R.string.pref_brfiletypes);

    EnumPreferenceDefinition<CacheLocation> CACHE_LOCATION = new EnumPreferenceDefinition<CacheLocation>(
            CacheLocation.class, R.string.pref_cachelocation_id, R.string.pref_cachelocation_defvalue);

    BooleanPreferenceDefinition AUTO_SCAN_REMOVABLE_MEDIA = new BooleanPreferenceDefinition(R.string.pref_autoscanremovable_id,
            R.string.pref_autoscanremovable_defvalue);

    BooleanPreferenceDefinition SHOW_REMOVABLE_MEDIA = new BooleanPreferenceDefinition(R.string.pref_showremovable_id,
            R.string.pref_showremovable_defvalue);

    BooleanPreferenceDefinition SHOW_SCANNING_MEDIA = new BooleanPreferenceDefinition(R.string.pref_showscanning_id,
            R.string.pref_showscanning_defvalue);

    BooleanPreferenceDefinition SHOW_NOTIFICATIONS = new BooleanPreferenceDefinition(R.string.pref_shownotifications_id,
            R.string.pref_shownotifications_defvalue);

}
