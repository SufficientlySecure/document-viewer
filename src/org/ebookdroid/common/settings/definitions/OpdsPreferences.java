package org.ebookdroid.common.settings.definitions;

import org.ebookdroid.R;
import org.ebookdroid.common.settings.base.JsonArrayPreferenceDefinition;

public interface OpdsPreferences {

    /* =============== OPDS settings =============== */

    JsonArrayPreferenceDefinition OPDS_CATALOGS = new JsonArrayPreferenceDefinition(R.string.pref_opdscatalogs_id);
}
