package org.ebookdroid.core.settings;

import org.ebookdroid.R;
import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.settings.SettingsManager.BookSettingsEditor;
import org.ebookdroid.core.utils.PathFromUri;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class BookSettingsActivity extends BaseSettingsActivity {

    private static final LogContext LCTX = LogContext.ROOT.lctx("Settings");

    private BookSettingsEditor edit;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Uri uri = getIntent().getData();
        final String fileName = PathFromUri.retrieve(getContentResolver(), uri);
        final BookSettings bs = SettingsManager.getBookSettings(fileName);
        if (bs == null) {
            finish();
        }

        edit = SettingsManager.edit(bs);

        try {
            addPreferencesFromResource(R.xml.books_prefs);
        } catch (final ClassCastException e) {
            LCTX.e("Book preferences are corrupt! Resetting to default values.");

            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            final SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();

            PreferenceManager.setDefaultValues(this, R.xml.books_prefs, true);
            addPreferencesFromResource(R.xml.books_prefs);
        }

        decoratePreferences("book_align", "book_animationType");
    }

    @Override
    protected void onPause() {
        if (edit != null) {
            edit.commit();
        }
        super.onPause();
    }
}
