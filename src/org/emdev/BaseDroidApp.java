package org.emdev;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;
import java.util.Properties;

import org.emdev.common.android.AndroidVersion;
import org.emdev.common.log.LogManager;
import org.emdev.utils.FileUtils;
import org.emdev.utils.LengthUtils;

public class BaseDroidApp extends Application {

    public static Context context;

    public static int APP_VERSION_CODE;

    public static String APP_VERSION_NAME;

    public static String APP_PACKAGE;

    public static File EXT_STORAGE;

    public static File APP_STORAGE;

    public static Properties BUILD_PROPS;

    public static boolean IS_EMULATOR;

    public static String APP_NAME;

    public static Locale defLocale;

    private static Locale appLocale;

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

        final Configuration config = context.getResources().getConfiguration();
        appLocale = defLocale = config.locale;

        BUILD_PROPS = new Properties();
        try {
            BUILD_PROPS.load(new FileInputStream("/system/build.prop"));
        } catch (final Throwable th) {
        }

        final PackageManager pm = getPackageManager();
        try {
            final PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
            APP_NAME = getString(pi.applicationInfo.labelRes);
            APP_VERSION_CODE = pi.versionCode;
            APP_VERSION_NAME = LengthUtils.safeString(pi.versionName, "DEV");
            APP_PACKAGE = pi.packageName;
            EXT_STORAGE = Environment.getExternalStorageDirectory();
            APP_STORAGE = getAppStorage(APP_PACKAGE);
            IS_EMULATOR = "sdk".equalsIgnoreCase(Build.MODEL);

            Log.i(APP_NAME, APP_NAME + " (" + APP_PACKAGE + ")" + " " + APP_VERSION_NAME + "(" + pi.versionCode + ")");

            Log.i(APP_NAME, "Root             dir: " + Environment.getRootDirectory());
            Log.i(APP_NAME, "Data             dir: " + Environment.getDataDirectory());
            Log.i(APP_NAME, "External storage dir: " + EXT_STORAGE);
            Log.i(APP_NAME, "App      storage dir: " + APP_STORAGE);
            Log.i(APP_NAME, "Files            dir: " + FileUtils.getAbsolutePath(getFilesDir()));
            Log.i(APP_NAME, "Cache            dir: " + FileUtils.getAbsolutePath(getCacheDir()));
            Log.i(APP_NAME, "System locale       : " + defLocale);

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

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        final Configuration oldConfig = getResources().getConfiguration();
        final int diff = oldConfig.diff(newConfig);
        final Configuration target = diff == 0 ? oldConfig : newConfig;

        if (appLocale != null) {
            setAppLocaleIntoConfiguration(target);
        }
        super.onConfigurationChanged(target);
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

    public static void setAppLocale(final String lang) {
        final Configuration config = context.getResources().getConfiguration();
        appLocale = LengthUtils.isNotEmpty(lang) ? new Locale(lang) : defLocale;
        setAppLocaleIntoConfiguration(config);
    }

    protected static void setAppLocaleIntoConfiguration(final Configuration config) {
        if (!config.locale.equals(appLocale)) {
            Locale.setDefault(appLocale);
            config.locale = appLocale;
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        }
        Log.i(APP_NAME, "UI Locale: " + appLocale);
    }
}
