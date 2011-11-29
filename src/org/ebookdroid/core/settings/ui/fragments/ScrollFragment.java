package org.ebookdroid.core.settings.ui.fragments;

import org.ebookdroid.R;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class ScrollFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.fragment_scroll);
    }
}