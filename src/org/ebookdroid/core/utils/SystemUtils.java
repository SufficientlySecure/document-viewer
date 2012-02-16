package org.ebookdroid.core.utils;

import org.ebookdroid.EBookDroidApp;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.List;

public class SystemUtils {

    private static final ComponentName SYSTEM_UI = new ComponentName("com.android.systemui",
            "com.android.systemui.SystemUIService");
    private static final String SU_PATH1 = "/system/bin/su";
    private static final String SU_PATH2 = "/system/xbin/su";

    private SystemUtils() {

    }

    private static String getSuPath() {
        File file = new File(SU_PATH1);
        if (file.exists() && file.isFile() && file.canExecute()) {
            return SU_PATH1;
        } else {
            File file1 = new File(SU_PATH2);
            if (file1.exists() && file1.isFile() && file1.canExecute())
                return SU_PATH2;
        }
        return null;
    }

    public static final boolean isSystemUIRunning() {
        ActivityManager actvityManager = (ActivityManager) EBookDroidApp.context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningServiceInfo> rsiList = actvityManager.getRunningServices(1000);

        for (RunningServiceInfo rsi : rsiList) {
            if (SYSTEM_UI.equals(rsi.service)) {
                return true;
            }
        }
        return false;
    }

    public static void startSystemUI() {
        if (isSystemUIRunning()) {
            return;
        }
        exec(new String[] { "/system/bin/am", "startservice", "-n", "com.android.systemui/.SystemUIService" });
    }

    public static boolean stopSystemUI() {
        if (!isSystemUIRunning()) {
            return true;
        }

        final String su = getSuPath();
        if (su == null) {
            return false;
        } else {
            exec(new String[] { su, "-c", "service call activity 79 s16 com.android.systemui" });
        }
        return true;
    }

    public static void toggleSystemUI() {
        if (isSystemUIRunning()) {
            stopSystemUI();
        } else {
            startSystemUI();
        }
    }

    public static void exec(final String... as) {
        (new Thread(new Runnable() {

            public void run() {
                execImpl(as);
            }
        })).start();
    }

    private static String execImpl(String... as) {
        try {
            Process process = Runtime.getRuntime().exec(as);
            InputStreamReader r = new InputStreamReader(process.getInputStream());
            StringWriter w = new StringWriter();
            char ac[] = new char[8192];
            int i = 0;
            do {
                i = r.read(ac, 0, 8192);
                w.write(ac, 0, i);
            } while (i != -1);
            r.close();
            process.waitFor();
            return w.toString();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

}
