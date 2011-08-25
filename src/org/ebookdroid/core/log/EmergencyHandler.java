package org.ebookdroid.core.log;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EmergencyHandler implements UncaughtExceptionHandler {

    private static final EmergencyHandler instance = new EmergencyHandler();

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd.HHmmss");

    private static String APP_VERSION;

    private static String APP_PACKAGE;

    private static String FILES_PATH;

    private static UncaughtExceptionHandler system;

    private EmergencyHandler() {
    }

    @Override
    public void uncaughtException(final Thread thread, final Throwable ex) {
        processException(ex);
        system.uncaughtException(thread, ex);
    }

    private void processException(final Throwable th) {
        try {
            final String timestamp = SDF.format(new Date());

            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);

            th.printStackTrace(printWriter);
            final String stacktrace = result.toString();
            printWriter.close();

            final String filename = FILES_PATH + "/" + APP_PACKAGE + "." + APP_VERSION + "." + timestamp
                    + ".stacktrace";

            writeToFile(stacktrace, filename);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private void writeToFile(final String stacktrace, final String filename) {
        try {
            final BufferedWriter bos = new BufferedWriter(new FileWriter(filename));
            bos.write(stacktrace);
            bos.flush();
            bos.close();
            System.out.println("Stacktrace is written: " + filename);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static void init(final Context context) {
        if (system == null) {
            final PackageManager pm = context.getPackageManager();
            try {
                final PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
                APP_VERSION = pi.versionName;
                APP_PACKAGE = pi.packageName;

                File dir = Environment.getExternalStorageDirectory();
                if (dir != null) {
                    File appDir = new File(dir, "." + APP_PACKAGE);
                    if (appDir.isDirectory() || appDir.mkdir()) {
                        dir = appDir;
                    }
                } else {
                    dir = context.getFilesDir();
                }
                FILES_PATH = dir.getAbsolutePath();
            } catch (final NameNotFoundException e) {
                e.printStackTrace();
            }
            system = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(instance);
        }
    }

    public static void onUnexpectedError(final Throwable th) {
        instance.processException(th);
    }
}
