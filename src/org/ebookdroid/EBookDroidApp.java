package org.ebookdroid;

import org.ebookdroid.core.log.EmergencyHandler;
import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.settings.SettingsManager;

import android.app.Application;


public class EBookDroidApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        EmergencyHandler.init(this);
        LogContext.init(this);
        SettingsManager.init(this);
    }
}
