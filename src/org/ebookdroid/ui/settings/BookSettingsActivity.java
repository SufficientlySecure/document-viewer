package org.ebookdroid.ui.settings;

import org.ebookdroid.R;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

import org.emdev.common.filesystem.PathFromUri;

public class BookSettingsActivity extends BaseSettingsActivity {

    private BookSettings current;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Uri uri = getIntent().getData();
        final String fileName = PathFromUri.retrieve(getContentResolver(), uri);
        current = SettingsManager.getBookSettings(fileName);
        if (current == null) {
            finish();
            return;
        }

        setRequestedOrientation(current.getOrientation(AppSettings.current()));

        SettingsManager.onBookSettingsActivityCreated(current);

        try {
            addPreferencesFromResource(R.xml.fragment_book);
        } catch (final ClassCastException e) {
            LCTX.e("Book preferences are corrupt! Resetting to default values.");

            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            final SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();

            PreferenceManager.setDefaultValues(this, R.xml.fragment_book, true);
            addPreferencesFromResource(R.xml.fragment_book);
        }

        decorator.decoratePreference(getRoot());
        decorator.decorateBooksSettings(current);
    }

    @Override
    protected void onPause() {
        SettingsManager.onBookSettingsActivityClosed(current);
        super.onPause();
    }
}
