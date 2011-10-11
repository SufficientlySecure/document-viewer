package org.ebookdroid.core;

import org.ebookdroid.cbdroid.CbrViewerActivity;
import org.ebookdroid.cbdroid.CbzViewerActivity;
import org.ebookdroid.djvudroid.DjvuViewerActivity;
import org.ebookdroid.pdfdroid.PdfViewerActivity;
import org.ebookdroid.xpsdroid.XpsViewerActivity;

import android.app.Activity;
import android.net.Uri;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public enum Activities {

    PDF(PdfViewerActivity.class, "pdf"),

    DJVU(DjvuViewerActivity.class, "djvu", "djv"),

    XPS(XpsViewerActivity.class, "xps", "oxps"),

    CBZ(CbzViewerActivity.class, "cbz"),

    CBR(CbrViewerActivity.class, "cbr"),

    FB2(null, "fb2", "fb2.zip");

    private final static Map<String, Class<? extends Activity>> extensionToActivity;

    static {
        extensionToActivity = new HashMap<String, Class<? extends Activity>>();
        for (final Activities a : values()) {
            final Class<? extends Activity> ac = a.getActivityClass();
            for (final String ext : a.getExtensions()) {
                extensionToActivity.put(ext.toLowerCase(), ac);
            }
        }
    }

    private final Class<? extends Activity> activityClass;

    private final String[] extensions;

    private Activities(final Class<? extends Activity> activityClass, final String... extensions) {
        this.activityClass = activityClass;
        this.extensions = extensions;
    }

    public Class<? extends Activity> getActivityClass() {
        return activityClass;
    }

    public String[] getExtensions() {
        return extensions;
    }

    public static Set<String> getAllExtensions() {
        return extensionToActivity.keySet();
    }

    public static Class<? extends Activity> getByUri(final Uri uri) {
        final String uriString = uri.toString();
        for (final String ext : extensionToActivity.keySet()) {
            if (uriString.endsWith("." + ext)) {
                return extensionToActivity.get(ext);
            }
        }
        return null;
    }

    public static Class<? extends Activity> getByExtension(final String ext) {
        return extensionToActivity.get(ext.toLowerCase());
    }
}
