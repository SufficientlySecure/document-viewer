package org.ebookdroid;

import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.log.EmergencyHandler;
import org.ebookdroid.common.log.LogContext;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.SettingsManager;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.emdev.utils.FileUtils;
import org.emdev.utils.android.AndroidVersion;
import org.emdev.utils.android.VMRuntimeHack;

public class EBookDroidApp extends Application {

    public static final LogContext LCTX = LogContext.ROOT;

    public static Context context;

    public static String APP_VERSION;

    public static String APP_PACKAGE;

    public static File EXT_STORAGE;

    public static File APP_STORAGE;

    public static Properties BUILD_PROPS;

    /**
     * {@inheritDoc}
     *
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();

        this.init();

        EmergencyHandler.init(this);
        LogContext.init(this);
        SettingsManager.init(this);
        CacheManager.init(this);

        VMRuntimeHack.preallocateHeap(AppSettings.current().heapPreallocate);
    }

    protected void init() {
        context = getApplicationContext();
        BUILD_PROPS = new Properties();
        try {
            BUILD_PROPS.load(new FileInputStream("/system/build.prop"));
        } catch (Throwable th) {
        }

        final PackageManager pm = getPackageManager();
        try {
            final PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
            APP_VERSION = pi.versionName;
            APP_PACKAGE = pi.packageName;
            EXT_STORAGE = Environment.getExternalStorageDirectory();
            APP_STORAGE = getAppStorage();

            LCTX.i(getString(pi.applicationInfo.labelRes) + " (" + APP_PACKAGE + ")" + " v" + APP_VERSION + "("
                    + pi.versionCode + ")");

            LCTX.i("Root             dir: " + Environment.getRootDirectory());
            LCTX.i("Data             dir: " + Environment.getDataDirectory());
            LCTX.i("External storage dir: " + EXT_STORAGE);
            LCTX.i("App      storage dir: " + APP_STORAGE);
            LCTX.i("Files            dir: " + FileUtils.getAbsolutePath(getFilesDir()));
            LCTX.i("Cache            dir: " + FileUtils.getAbsolutePath(getCacheDir()));

            LCTX.i("VERSION     : " + AndroidVersion.VERSION);
            LCTX.i("BOARD       : " + Build.BOARD);
            LCTX.i("BRAND       : " + Build.BRAND);
            LCTX.i("CPU_ABI     : " + BUILD_PROPS.getProperty("ro.product.cpu.abi"));
            LCTX.i("CPU_ABI2    : " + BUILD_PROPS.getProperty("ro.product.cpu.abi2"));
            LCTX.i("DEVICE      : " + Build.DEVICE);
            LCTX.i("DISPLAY     : " + Build.DISPLAY);
            LCTX.i("FINGERPRINT : " + Build.FINGERPRINT);
            LCTX.i("ID          : " + Build.ID);
            LCTX.i("MANUFACTURER: " + BUILD_PROPS.getProperty("ro.product.manufacturer"));
            LCTX.i("MODEL       : " + Build.MODEL);
            LCTX.i("PRODUCT     : " + Build.PRODUCT);
        } catch (final NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Application#onLowMemory()
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        BitmapManager.clear("on Low Memory: ");
    }

    private File getAppStorage() {
        File dir = EXT_STORAGE;
        if (dir != null) {
            File appDir = new File(dir, "." + EBookDroidApp.APP_PACKAGE);
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
