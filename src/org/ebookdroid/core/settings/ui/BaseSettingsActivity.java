package org.ebookdroid.core.settings.ui;

import org.ebookdroid.core.log.LogContext;

import android.preference.PreferenceActivity;

public class BaseSettingsActivity extends PreferenceActivity implements IPreferenceContainer {

    public static final LogContext LCTX = LogContext.ROOT.lctx("Settings");

    protected final PreferencesDecorator decorator = new PreferencesDecorator(this);
}
