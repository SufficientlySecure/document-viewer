package org.sufficientlysecure.viewer.fontpack;

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

	private static final String VIEWER_PACKAGE = "org.sufficientlysecure.viewer";

	public static File APP_STORAGE;
	public static int VERSION;

	public static SystemFontProvider sfm;
	public static AssetsFontProvider afm;
	public static ExtStorageFontProvider esfm;

	@Override
	public void onCreate() {
		super.onCreate();

		VERSION = -1;
		final PackageManager pm = getPackageManager();
		try {
			final PackageInfo pi = pm.getPackageInfo(VIEWER_PACKAGE, 0);
			VERSION = pi.versionCode;
			Log.i(APP_NAME, "Document Viewer installed: v" + pi.versionName
					+ " (" + VERSION + ")");
		} catch (NameNotFoundException ex) {
			Log.w(APP_NAME, "Document Viewer is not installed");
		}

		APP_STORAGE = getAppStorage(VIEWER_PACKAGE);

		sfm = new SystemFontProvider();
		sfm.init();

		afm = new AssetsFontProvider();
		afm.init();

		esfm = new ExtStorageFontProvider(APP_STORAGE);
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
