package org.ebookdroid.ui.settings;

import org.ebookdroid.R;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.SettingsManager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class SettingsActivity extends BaseSettingsActivity {

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (SettingsManager.getBookSettings() != null) {
            setRequestedOrientation(AppSettings.current().rotation.getOrientation());
        }
        onCreate();
    }

    @Override
    protected void onPause() {
        SettingsManager.onSettingsChanged();
        super.onPause();
    }

    protected void onCreate() {
        try {
            addPreferencesFromResource(R.xml.preferences);

        } catch (final ClassCastException e) {
            LCTX.e("Shared preferences are corrupt! Resetting to default values.");

            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            final SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();

            PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
            addPreferencesFromResource(R.xml.preferences);
        }

        decorator.decorateSettings();

        if (SettingsManager.getBookSettings() == null) {
            final Preference bookPrefs = findPreference("book_prefs");
            if (bookPrefs != null) {
                final PreferenceScreen preferenceScreen = getPreferenceScreen();
                preferenceScreen.removePreference(bookPrefs);
            }
        }
    }
}
