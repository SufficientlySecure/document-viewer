package org.ebookdroid.core.settings;

import org.ebookdroid.R;
import org.ebookdroid.core.settings.SettingsManager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
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

        if (SettingsManager.getInstance(this).getBookSettings() == null) {
            Preference somePreference = findPreference("book_render");
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            preferenceScreen.removePreference(somePreference);
        }
    }

}
