package org.ebookdroid;

import org.ebookdroid.core.log.EmergencyHandler;
import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.settings.SettingsManager;

import android.app.Application;
import android.content.Context;

public class EBookDroidApp extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        EmergencyHandler.init(this);
        LogContext.init(this);
        SettingsManager.init(this);
        EBookDroidApp.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return context;
    }
}
