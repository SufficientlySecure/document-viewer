package org.emdev.common.log;

import android.util.Log;

public class LogContext {

    private static final String SEPARATOR = ".";

    private final LogContext parent;

    private final String tag;

    private Boolean debugEnabled;

    LogContext(final String tag, final boolean debugEnabled) {
        this.parent = null;
        this.tag = tag;
        this.debugEnabled = debugEnabled;
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

    public void w(final String msg) {
        Log.w(tag, msg);
    }

    public void w(final String msg, final Throwable th) {
        Log.w(tag, msg, th);
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
}
