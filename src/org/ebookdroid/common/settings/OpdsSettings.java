package org.ebookdroid.common.settings;

import org.ebookdroid.common.settings.definitions.OpdsPreferences;

import android.content.SharedPreferences;

import org.json.JSONArray;

public class OpdsSettings implements OpdsPreferences {

    /* =============== OPDS settings =============== */

    public final JSONArray opdsCatalogs;
    public final String downloadDir;
    public final boolean filterTypes;
    public final boolean downloadArchives;
    public final boolean unpackArchives;
    public final boolean deleteArchives;

    OpdsSettings(final SharedPreferences prefs) {
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
