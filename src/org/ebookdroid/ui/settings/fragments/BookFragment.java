package org.ebookdroid.ui.settings.fragments;

import org.ebookdroid.R;

import android.annotation.TargetApi;

@TargetApi(11)
public class BookFragment extends BasePreferenceFragment {

    public BookFragment() {
        super(R.xml.fragment_book);
    }

    @Override
    public void decorate() {
        super.decorate();
        decorator.decorateBooksSettings();
    }
}
