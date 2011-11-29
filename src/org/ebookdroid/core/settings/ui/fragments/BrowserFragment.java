package org.ebookdroid.core.settings.ui.fragments;

import org.ebookdroid.R;

public class BrowserFragment extends BasePreferenceFragment {

    public BrowserFragment() {
        super(R.xml.fragment_browser);
    }

    @Override
    public void decorate() {
        decorator.decorateBrowserSettings();
    }

}
