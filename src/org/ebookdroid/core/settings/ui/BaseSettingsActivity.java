package org.ebookdroid.core.settings.ui;

import android.preference.PreferenceActivity;

public class BaseSettingsActivity extends PreferenceActivity implements IPreferenceContainer {

    protected final PreferencesDecorator decorator = new PreferencesDecorator(this);
}
