package org.ebookdroid.ui.settings.fragments;

import org.ebookdroid.R;

import android.annotation.TargetApi;

@TargetApi(11)
public class MemoryFragment extends BasePreferenceFragment {

    public MemoryFragment() {
        super(R.xml.fragment_memory);
    }

    @Override
    public void decorate() {
        decorator.decorateMemorySettings();
    }
}
