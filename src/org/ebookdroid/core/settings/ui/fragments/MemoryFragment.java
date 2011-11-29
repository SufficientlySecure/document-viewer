package org.ebookdroid.core.settings.ui.fragments;

import org.ebookdroid.R;

public class MemoryFragment extends BasePreferenceFragment {

    public MemoryFragment() {
        super(R.xml.fragment_memory);
    }

    @Override
    public void decorate() {
        decorator.decorateMemorySettings();
    }
}
