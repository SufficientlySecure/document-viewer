package org.ebookdroid.fontpack;

import java.io.File;

import org.emdev.BaseDroidApp;
import org.emdev.common.fonts.AssetsFontProvider;
import org.emdev.common.fonts.ExtStorageFontProvider;
import org.emdev.common.fonts.SystemFontProvider;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;

public class FontpackApp extends BaseDroidApp {

    private static final String EBOOKDROID_PACKAGE = "org.ebookdroid";

    public static File EBOOKDROID_APP_STORAGE;
    public static int EBOOKDROID_VERSION;

    public static SystemFontProvider sfm;
    public static AssetsFontProvider afm;
    public static ExtStorageFontProvider esfm;

    @Override
    public void onCreate() {
        super.onCreate();

        EBOOKDROID_VERSION = -1;
        final PackageManager pm = getPackageManager();
        try {
            final PackageInfo pi = pm.getPackageInfo(EBOOKDROID_PACKAGE, 0);
            EBOOKDROID_VERSION = pi.versionCode;
            Log.i(APP_NAME, "EBookDroid installed: v" + pi.versionName + " (" + EBOOKDROID_VERSION + ")");
        } catch (NameNotFoundException ex) {
            Log.w(APP_NAME, "EBookDroid is not installed");
        }

        EBOOKDROID_APP_STORAGE = getAppStorage(EBOOKDROID_PACKAGE);

        sfm = new SystemFontProvider();
        sfm.init();

        afm = new AssetsFontProvider();
        afm.init();

        esfm = new ExtStorageFontProvider(EBOOKDROID_APP_STORAGE);
        esfm.init();
    }

    public static void uninstall() {
        Intent intent = new Intent(Intent.ACTION_DELETE);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        System.out.println(intent);
        context.startActivity(intent);
    }
}
