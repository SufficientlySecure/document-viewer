package org.ebookdroid.ui.viewer.viewers;

import org.ebookdroid.common.settings.AppSettings;

import android.app.Activity;
import android.os.Handler;
import android.view.View;

import java.util.concurrent.atomic.AtomicBoolean;

import org.emdev.common.android.AndroidVersion;
import org.emdev.ui.uimanager.IUIManager;

public class FullScreenCallback implements Runnable {

    private static final int TIMEOUT = 2000;

    protected final AtomicBoolean added = new AtomicBoolean();

    protected final Activity activity;

    protected final View view;

    protected volatile long time;

    private FullScreenCallback(final Activity activity, final View view) {
        this.activity = activity;
        this.view = view;
    }

    public static FullScreenCallback get(final Activity activity, final View view) {
        return AndroidVersion.is4x ?
        /* Creates full-screen callback devices with Android 4.x */
        new FullScreenCallback(activity, view) : null;
    }

    @Override
    public void run() {
        if (!AppSettings.current().fullScreen) {
            // System.out.println("fullScreenCallback: full-screen mode off");
            added.set(false);
            return;
        }

        final long now = System.currentTimeMillis();

        // Check if checkFullScreenMode() was called
        if (added.compareAndSet(false, true)) {
            // Only adds delayed message
            this.time = System.currentTimeMillis();
            // System.out.println("fullScreenCallback: postDelayed(): " + TIMEOUT);
            final Handler handler = view != null ? view.getHandler() : null;
            if (handler != null) {
                handler.postDelayed(this, TIMEOUT);
            }
            return;
        }

        // Process delayed message
        final long expected = time + TIMEOUT;
        if (expected <= now) {
            // System.out.println("fullScreenCallback: setFullScreenMode()");
            if (view != null) {
                IUIManager.instance.setFullScreenMode(activity, view, true);
            }
            added.set(false);
            return;
        }

        final Handler handler = view != null ? view.getHandler() : null;
        if (handler != null) {
            added.set(true);
            final long delta = expected - now;
            // System.out.println("fullScreenCallback: postDelayed(): " + delta);
            handler.postDelayed(this, delta);
            return;
        }

        added.set(false);
    }

    public void checkFullScreenMode() {
        // System.out.println("fullScreenCallback: checkFullScreenMode()");
        this.time = System.currentTimeMillis();
        if (!added.get()) {
            if (view != null) {
                view.post(this);
            }
        }
    }
}
