package org.ebookdroid.core.settings.ui.fragments;

import org.ebookdroid.core.settings.ui.IPreferenceContainer;
import org.ebookdroid.core.settings.ui.PreferencesDecorator;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class BasePreferenceFragment extends PreferenceFragment implements IPreferenceContainer {

    protected final PreferencesDecorator decorator;

    protected final int fragmentId;

    public BasePreferenceFragment(final int fragmentId) {
        this.fragmentId = fragmentId;
        this.decorator = new PreferencesDecorator(this);
    }

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(fragmentId);

        decorate();
    }

    public void decorate() {
    }
}
