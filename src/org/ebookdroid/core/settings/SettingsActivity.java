package org.ebookdroid.core.settings;

import org.ebookdroid.R;
import org.ebookdroid.core.DecodeMode;
import org.ebookdroid.core.log.LogContext;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class SettingsActivity extends BaseSettingsActivity {

    private static final LogContext LCTX = LogContext.ROOT.lctx("Settings");

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        addListener("decodemode", new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                enableMaxImageSizePref(DecodeMode.getByResValue(newValue.toString()));
                return true;
            }
        });


        decoratePreferences("rotation", "brightness");
        decoratePreferences("tapsize", "scrollheight");
        decoratePreferences("pagesinmemory", "decodemode", "maximagesize");
        decoratePreferences("brautoscandir");
        decoratePreferences("align", "animationType");
        decoratePreferences("book_align", "book_animationType");
        decoratePreferences("djvu_rendering_mode");
        
        if (SettingsManager.getBookSettings() == null) {
            final Preference bookPrefs = findPreference("book_prefs");
            if (bookPrefs != null) {
                final PreferenceScreen preferenceScreen = getPreferenceScreen();
                preferenceScreen.removePreference(bookPrefs);
            }
        }

        addListener("animationType", new AnimationTypeListener("align"));
        addListener("book_animationType", new AnimationTypeListener("book_align"));

        enableMaxImageSizePref(SettingsManager.getAppSettings().getDecodeMode());
    }

    @Override
    protected void onPause() {
        SettingsManager.onSettingsChanged();
        super.onPause();
    }

    protected void enableMaxImageSizePref(final DecodeMode decodeMode) {
        final Preference pref = findPreference("maximagesize");
        pref.setEnabled(DecodeMode.LOW_MEMORY == decodeMode);
    }

}
