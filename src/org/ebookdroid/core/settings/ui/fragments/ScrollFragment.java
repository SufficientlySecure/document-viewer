package org.ebookdroid.core.settings.ui.fragments;

import org.ebookdroid.R;

public class ScrollFragment extends BasePreferenceFragment {

    public ScrollFragment() {
        super(R.xml.fragment_scroll);
    }

    @Override
    public void decorate() {
        decorator.decorateScrollSettings();
    }

}
