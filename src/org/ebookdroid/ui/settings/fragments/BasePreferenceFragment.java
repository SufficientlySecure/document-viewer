package org.ebookdroid.ui.settings.fragments;

import org.ebookdroid.ui.settings.IPreferenceContainer;
import org.ebookdroid.ui.settings.PreferencesDecorator;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

@TargetApi(11)
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
        decorator.decoratePreference(getRoot());
    }

    @Override
    public Preference getRoot() {
        return this.getPreferenceScreen();
    }
}
