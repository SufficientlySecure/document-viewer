package org.ebookdroid.ui.settings;

import android.preference.Preference;


public interface IPreferenceContainer {

    Preference getRoot();

    Preference findPreference(CharSequence key);
}
