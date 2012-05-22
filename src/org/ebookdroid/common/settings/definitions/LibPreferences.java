package org.ebookdroid.common.settings.definitions;

import static org.ebookdroid.R.string.pref_brautoscandir_defvalue;
import static org.ebookdroid.R.string.pref_brautoscandir_id;
import static org.ebookdroid.R.string.pref_brsearchbookquery_id;
import static org.ebookdroid.R.string.pref_usebookcase_defvalue;
import static org.ebookdroid.R.string.pref_usebookcase_id;

import org.ebookdroid.common.settings.base.BooleanPreferenceDefinition;
import org.ebookdroid.common.settings.base.FileListPreferenceDefinition;
import org.ebookdroid.common.settings.base.FileTypeFilterPreferenceDefinition;
import org.ebookdroid.common.settings.base.StringPreferenceDefinition;

public interface LibPreferences {

    /* =============== Browser settings =============== */

    BooleanPreferenceDefinition USE_BOOK_CASE = new BooleanPreferenceDefinition(pref_usebookcase_id,
            pref_usebookcase_defvalue);

    FileListPreferenceDefinition AUTO_SCAN_DIRS = new FileListPreferenceDefinition(pref_brautoscandir_id,
            pref_brautoscandir_defvalue);

    StringPreferenceDefinition SEARCH_BOOK_QUERY = new StringPreferenceDefinition(pref_brsearchbookquery_id, 0);

    FileTypeFilterPreferenceDefinition FILE_TYPE_FILTER = new FileTypeFilterPreferenceDefinition("brfiletype");

}
