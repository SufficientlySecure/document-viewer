package org.ebookdroid.ui.settings;

import android.app.Activity;
import android.preference.Preference;


public interface IPreferenceContainer {

    Preference getRoot();

    Preference findPreference(CharSequence key);

    Activity getActivity();
}
