package org.ebookdroid.ui.settings;


import android.app.Activity;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;

public class BaseSettingsActivity extends PreferenceActivity implements IPreferenceContainer {

    public static final LogContext LCTX = LogManager.root().lctx("Settings");

    protected final PreferencesDecorator decorator = new PreferencesDecorator(this);

    @Override
    public Preference getRoot() {
        return this.getPreferenceScreen();
    }

    @Override
    public Activity getActivity() {
        return this;
    }
}
