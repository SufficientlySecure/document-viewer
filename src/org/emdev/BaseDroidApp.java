package org.emdev;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.emdev.common.android.AndroidVersion;
import org.emdev.common.log.LogManager;
import org.emdev.utils.FileUtils;

public class BaseDroidApp extends Application {

    public static Context context;

    public static String APP_VERSION;

    public static String APP_PACKAGE;

    public static File EXT_STORAGE;

    public static File APP_STORAGE;

    public static Properties BUILD_PROPS;

    public static String APP_NAME;

    /**
     * {@inheritDoc}
     *
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();

        this.init();

        LogManager.init(this);
    }

    protected void init() {
        context = getApplicationContext();
        BUILD_PROPS = new Properties();
        try {
            BUILD_PROPS.load(new FileInputStream("/system/build.prop"));
        } catch (final Throwable th) {
        }

        final PackageManager pm = getPackageManager();
        try {
            final PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
            APP_NAME = getString(pi.applicationInfo.labelRes);
            APP_VERSION = pi.versionName;
            APP_PACKAGE = pi.packageName;
            EXT_STORAGE = Environment.getExternalStorageDirectory();
            APP_STORAGE = getAppStorage(APP_PACKAGE);

            Log.i(APP_NAME, APP_NAME + " (" + APP_PACKAGE + ")" + " v" + APP_VERSION + "(" + pi.versionCode + ")");

            Log.i(APP_NAME, "Root             dir: " + Environment.getRootDirectory());
            Log.i(APP_NAME, "Data             dir: " + Environment.getDataDirectory());
            Log.i(APP_NAME, "External storage dir: " + EXT_STORAGE);
            Log.i(APP_NAME, "App      storage dir: " + APP_STORAGE);
            Log.i(APP_NAME, "Files            dir: " + FileUtils.getAbsolutePath(getFilesDir()));
            Log.i(APP_NAME, "Cache            dir: " + FileUtils.getAbsolutePath(getCacheDir()));

            Log.i(APP_NAME, "VERSION     : " + AndroidVersion.VERSION);
            Log.i(APP_NAME, "BOARD       : " + Build.BOARD);
            Log.i(APP_NAME, "BRAND       : " + Build.BRAND);
            Log.i(APP_NAME, "CPU_ABI     : " + BUILD_PROPS.getProperty("ro.product.cpu.abi"));
            Log.i(APP_NAME, "CPU_ABI2    : " + BUILD_PROPS.getProperty("ro.product.cpu.abi2"));
            Log.i(APP_NAME, "DEVICE      : " + Build.DEVICE);
            Log.i(APP_NAME, "DISPLAY     : " + Build.DISPLAY);
            Log.i(APP_NAME, "FINGERPRINT : " + Build.FINGERPRINT);
            Log.i(APP_NAME, "ID          : " + Build.ID);
            Log.i(APP_NAME, "MANUFACTURER: " + BUILD_PROPS.getProperty("ro.product.manufacturer"));
            Log.i(APP_NAME, "MODEL       : " + Build.MODEL);
            Log.i(APP_NAME, "PRODUCT     : " + Build.PRODUCT);
        } catch (final NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    protected File getAppStorage(final String appPackage) {
        File dir = EXT_STORAGE;
        if (dir != null) {
            final File appDir = new File(dir, "." + appPackage);
            if (appDir.isDirectory() || appDir.mkdir()) {
                dir = appDir;
            }
        } else {
            dir = context.getFilesDir();
        }
        dir.mkdirs();
        return dir.getAbsoluteFile();
    }
}
