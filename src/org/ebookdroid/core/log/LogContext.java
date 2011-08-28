package org.ebookdroid.core.log;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class LogContext {

    public static final LogContext ROOT = new LogContext("EBookDroid");

    private static final String SEPARATOR = ".";

    private final LogContext parent;

    private final String tag;

    private Boolean debugEnabled;

    private LogContext(final String tag) {
        this(null, tag);
    }

    private LogContext(final LogContext parent, final String tag) {
        this.parent = parent;
        this.tag = (parent != null ? parent.tag + SEPARATOR : "") + tag;
    }

    public LogContext lctx(final String tag) {
        return new LogContext(this, tag);
    }

    public LogContext lctx(final String tag, final boolean debugEnabled) {
        final LogContext lctx = new LogContext(this, tag);
        lctx.setDebugEnabled(debugEnabled);
        return lctx;
    }

    public void d(final String msg) {
        Log.d(tag, msg);
    }

    public void d(final String msg, final Throwable th) {
        Log.d(tag, msg, th);
    }

    public void i(final String msg) {
        Log.i(tag, msg);
    }

    public void i(final String msg, final Throwable th) {
        Log.i(tag, msg, th);
    }

    public void e(final String msg) {
        Log.e(tag, msg);
    }

    public void e(final String msg, final Throwable th) {
        Log.e(tag, msg, th);
    }

    public boolean isDebugEnabled() {
        return debugEnabled != null ? debugEnabled.booleanValue() : parent != null ? parent.isDebugEnabled() : false;
    }

    public boolean setDebugEnabled(final boolean enabled) {
        return debugEnabled = enabled;
    }

    @Override
    public String toString() {
        return tag;
    }

    public static void init(final Context context) {
        final PackageManager pm = context.getPackageManager();
        try {
            final PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            ROOT.debugEnabled = (pi.applicationInfo.flags & (ApplicationInfo.FLAG_DEBUGGABLE | 0x100 /*ApplicationInfo.FLAG_TEST_ONLY*/)) != 0;
        } catch (final NameNotFoundException ex) {
            ex.printStackTrace();
        }
            Log.i(ROOT.tag, "Debug logging " + (ROOT.debugEnabled ? "enabled" : "disabled") + " by default");
    }
}
