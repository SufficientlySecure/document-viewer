package org.ebookdroid.core;

import org.ebookdroid.R;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            addPreferencesFromResource(R.xml.preferences);
        } catch (final ClassCastException e) {
            Log.e("VuDroidSettings", "Shared preferences are corrupt! Resetting to default values.");

            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

            final SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();

            PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
            addPreferencesFromResource(R.xml.preferences);
        }

    }

}
