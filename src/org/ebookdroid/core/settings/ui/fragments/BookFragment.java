package org.ebookdroid.core.settings.ui.fragments;

import org.ebookdroid.R;

public class BookFragment extends BasePreferenceFragment {

    public BookFragment() {
        super(R.xml.fragment_book);
    }

    @Override
    public void decorate() {
        decorator.decorateBooksSettings();
    }
}
