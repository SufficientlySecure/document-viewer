package org.ebookdroid.common.settings.definitions;

import org.ebookdroid.R;

import org.emdev.common.settings.base.BooleanPreferenceDefinition;
import org.emdev.common.settings.base.JsonArrayPreferenceDefinition;
import org.emdev.common.settings.base.StringPreferenceDefinition;

public interface OpdsPreferences {

    /* =============== OPDS settings =============== */

    JsonArrayPreferenceDefinition OPDS_CATALOGS = new JsonArrayPreferenceDefinition(R.string.pref_opds_catalogs_id);

    StringPreferenceDefinition OPDS_DOWNLOAD_DIR = new StringPreferenceDefinition(R.string.pref_opds_downloaddir_id,
            R.string.pref_opds_downloaddir_defvalue);

    BooleanPreferenceDefinition OPDS_FILTER_TYPES = new BooleanPreferenceDefinition(
            R.string.pref_opds_filterbooktypes_id, R.string.pref_opds_filterbooktypes_defvalue);

    BooleanPreferenceDefinition OPDS_DOWNLOAD_ARCHIVES = new BooleanPreferenceDefinition(
            R.string.pref_opds_downloadarchives_id, R.string.pref_opds_downloadarchives_defvalue);

    BooleanPreferenceDefinition OPDS_UNPACK_ARCHIVES = new BooleanPreferenceDefinition(
            R.string.pref_opds_unpackarchives_id, R.string.pref_opds_unpackarchives_defvalue);

    BooleanPreferenceDefinition OPDS_DELETE_ARCHIVES = new BooleanPreferenceDefinition(
            R.string.pref_opds_deletearchives_id, R.string.pref_opds_deletearchives_defvalue);

}
