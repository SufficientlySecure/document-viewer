package org.ebookdroid.ui.settings;

import org.ebookdroid.R;

import android.annotation.TargetApi;

import java.util.List;

@TargetApi(11)
public class FragmentedSettingsActivity extends SettingsActivity {


    @Override
    protected void onCreate() {
    }

    @Override
    public void onBuildHeaders(final List<Header> target) {
        loadHeadersFromResource(R.xml.preferences_headers, target);
    }
}
