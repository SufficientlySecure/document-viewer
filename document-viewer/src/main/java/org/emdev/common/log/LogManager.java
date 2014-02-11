package org.emdev.common.log;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import java.io.File;

import org.emdev.BaseDroidApp;

public final class LogManager {

    static File LOG_STORAGE;

    static LogContext root;

    static EmergencyHandler handler;

    private LogManager() {
    }

    public static void init(final Context context) {
        LOG_STORAGE = new File(BaseDroidApp.APP_STORAGE, "logs");
        LOG_STORAGE.mkdirs();

        final boolean debugEnabled = isDebugEnabledByDefault(context);
        Log.i(BaseDroidApp.APP_NAME, "Debug logging " + (debugEnabled ? "enabled" : "disabled") + " by default");

        root = new LogContext(BaseDroidApp.APP_NAME, debugEnabled);
        handler = new EmergencyHandler();
    }

    private static boolean isDebugEnabledByDefault(final Context context) {
        boolean debugEnabled = false;
        final PackageManager pm = context.getPackageManager();
        try {
            final PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            final int flags = ApplicationInfo.FLAG_DEBUGGABLE | 0x100 /* ApplicationInfo.FLAG_TEST_ONLY */;
            debugEnabled = (pi.applicationInfo.flags & flags) != 0;
        } catch (final NameNotFoundException ex) {
            ex.printStackTrace();
        }
        return debugEnabled;
    }

    public static LogContext root() {
        if (root == null) {
            root = new LogContext(BaseDroidApp.APP_PACKAGE, false);
        }
        return root;
    }

    public static void onUnexpectedError(final Throwable th) {
        handler.processException(th);
    }
}
