package org.ebookdroid.core.settings.ui;

import org.ebookdroid.R;
import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.utils.AndroidVersion;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import java.util.List;

public class SettingsActivity extends BaseSettingsActivity {

    private static final LogContext LCTX = LogContext.ROOT.lctx("Settings");

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AndroidVersion.lessThan3x) {
            return;
        }

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

        postCreate();
    }

    @Override
    protected void onPause() {
        SettingsManager.onSettingsChanged();
        super.onPause();
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preferences_headers, target);

        postCreate();
    }

    protected void postCreate() {

        if (AndroidVersion.lessThan3x) {
            decorator.decorateSettings();
        }

        if (SettingsManager.getBookSettings() == null) {
            final Preference bookPrefs = findPreference("book_prefs");
            if (bookPrefs != null) {
                final PreferenceScreen preferenceScreen = getPreferenceScreen();
                preferenceScreen.removePreference(bookPrefs);
            }
        }
    }
}
