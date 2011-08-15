package org.ebookdroid.core;

import org.ebookdroid.core.settings.SettingsManager;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import java.io.File;

public interface IBrowserActivity {

    Context getContext();

    Activity getActivity();

    SettingsManager getSettings();

    void setCurrentDir(File newDir);

    void showDocument(Uri uri);

}
