package org.ebookdroid;

//import org.ebookdroid.core.log.EmergencyHandler;
import org.ebookdroid.core.log.LogContext;

import android.app.Application;


public class EBookDroidApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //Disable EmergencyHandler. Currently not usefull, couse store file in private space
        //Also FC in some devices.
        //Need additional work.
        //EmergencyHandler.init(this);
        LogContext.init(this);
    }
}
