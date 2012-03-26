package org.ebookdroid.ui.settings;

import android.preference.Preference;


public interface IPreferenceContainer {

    Preference findPreference(CharSequence key);
}
