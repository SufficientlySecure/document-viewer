package org.ebookdroid.ui.settings;

import org.ebookdroid.R;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.ui.settings.fragments.BookFragment;

import android.annotation.TargetApi;

import java.util.Iterator;
import java.util.List;

@TargetApi(11)
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
