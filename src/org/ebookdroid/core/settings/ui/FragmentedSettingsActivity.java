package org.ebookdroid.core.settings.ui;

import org.ebookdroid.R;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.settings.ui.fragments.BookFragment;

import java.util.Iterator;
import java.util.List;

public class FragmentedSettingsActivity extends SettingsActivity {

    @Override
    protected void onCreate() {
    }

    @Override
    public void onBuildHeaders(final List<Header> target) {
        loadHeadersFromResource(R.xml.preferences_headers, target);

        if (SettingsManager.getBookSettings() == null) {
            for (Iterator<Header> i = target.iterator(); i.hasNext();) {
                Header header = i.next();
                if (BookFragment.class.getName().equals(header.fragment)) {
                    i.remove();
                    break;
                }
            }
        }
    }
}
