package org.ebookdroid.core;

import org.ebookdroid.core.settings.SettingsManager;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;


public interface IBrowserActivity {

    Context getContext();

    Activity getActivity();

    SettingsManager getSettings();

    void showDocument(Uri uri);

}
