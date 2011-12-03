package org.ebookdroid.core.settings.ui;

import org.ebookdroid.core.utils.AndroidVersion;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;

public class SettingsUI {

    public static void showBookSettings(final Context context, final String fileName) {
        final Intent bsa = new Intent(context, BookSettingsActivity.class);
        bsa.setData(Uri.fromFile(new File(fileName)));
        context.startActivity(bsa);
    }

    public static void showAppSettings(final Context context) {
        final Class<?> activityClass = AndroidVersion.lessThan3x ? SettingsActivity.class
                : FragmentedSettingsActivity.class;
        context.startActivity(new Intent(context, activityClass));
    }
}
