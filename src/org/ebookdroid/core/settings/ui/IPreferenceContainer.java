package org.ebookdroid.core.settings.ui;

import android.preference.Preference;


public interface IPreferenceContainer {

    Preference findPreference(CharSequence key);
}
