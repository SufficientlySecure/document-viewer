package org.ebookdroid.ui.settings.fragments;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.R;

import afzkl.development.mColorPicker.ColorPickerDialog;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;

@TargetApi(11)
public class UIFragment extends BasePreferenceFragment implements OnPreferenceClickListener {
    private Preference linkHighlightPreference;

    public UIFragment() {
        super(R.xml.fragment_ui);
    }

    @Override
    public void decorate() {
        decorator.decorateUISettings();
        setUp();
    }
    private void setUp() {
        linkHighlightPreference = findPreference("link_highlight");
        if (linkHighlightPreference != null) {
            linkHighlightPreference.setOnPreferenceClickListener(this);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        final SharedPreferences prefs = PreferenceManager
        .getDefaultSharedPreferences(EBookDroidApp.context);
        if (key.equals("link_highlight")) {

            final ColorPickerDialog d = new ColorPickerDialog(this.getActivity(), prefs.getInt("link_highlight", 0xffffffff));
            d.setAlphaSliderVisible(true);

            d.setButton("Ok", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("link_highlight", d.getColor());
                    editor.commit();

                }
            });

            d.setButton2("Cancel", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });

            d.show();

            return true;
        }
        return false;
    }
}
