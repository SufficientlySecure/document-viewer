package org.ebookdroid.common.settings;

import org.ebookdroid.common.settings.definitions.OpdsPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONArray;

public class OpdsSettings implements OpdsPreferences {

    final SharedPreferences prefs;

    /* =============== OPDS settings =============== */

    public final JSONArray opdsCatalogs;

    OpdsSettings(final Context context) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        /* =============== OPDS settings =============== */
        opdsCatalogs = OPDS_CATALOGS.getPreferenceValue(prefs);
    }

    /* =============== OPDS settings =============== */

    /* =============== */

    public static class Diff {

        private int mask;
        private final boolean firstTime;

        public Diff(final OpdsSettings olds, final OpdsSettings news) {
            firstTime = olds == null;
            if (firstTime) {
                mask = 0xFFFFFFFF;
            } else if (news != null) {
                mask = 0x00000000;
            }
        }

        public boolean isFirstTime() {
            return firstTime;
        }
    }
}
