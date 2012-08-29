package org.ebookdroid.ui.settings;

import org.ebookdroid.R;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.SettingsManager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import org.emdev.common.android.AndroidVersion;
import org.emdev.common.fonts.FontManager;

public class SettingsActivity extends BaseSettingsActivity {

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FontManager.init();
        if (getIntent().getData() != null) {
            setRequestedOrientation(AppSettings.current().rotation.getOrientation());
        }
        onCreate();
    }

    @Override
    protected void onPause() {
        SettingsManager.onSettingsChanged();
        super.onPause();
    }

    @SuppressWarnings("deprecation")
    protected void onCreate() {
        try {
            setPreferenceScreen(createPreferences());
        } catch (final ClassCastException e) {
            LCTX.e("Shared preferences are corrupt! Resetting to default values.");

            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            final SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();

            setPreferenceScreen(createPreferences());
        }

        decorator.decorateSettings();
    }

    @SuppressWarnings("deprecation")
    PreferenceScreen createPreferences() {
        final PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

        loadPreferences(root, R.xml.fragment_ui);
        loadPreferences(root, R.xml.fragment_scroll);
        loadPreferences(root, R.xml.fragment_memory);
        loadPreferences(root, R.xml.fragment_render);
        loadPreferences(root, R.xml.fragment_typespec);
        loadPreferences(root, R.xml.fragment_browser);

        if (AndroidVersion.VERSION >= 8) {
            loadPreferences(root, R.xml.fragment_opds);
        }

        loadPreferences(root, R.xml.fragment_backup);

        return root;
    }

    @SuppressWarnings("deprecation")
    void loadPreferences(final PreferenceScreen root, final int... resourceIds) {
        for (final int id : resourceIds) {
            setPreferenceScreen(null);
            addPreferencesFromResource(id);
            root.addPreference(getPreferenceScreen());
            setPreferenceScreen(null);
        }
    }

}
