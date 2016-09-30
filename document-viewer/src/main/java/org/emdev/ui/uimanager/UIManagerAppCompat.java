package org.emdev.ui.uimanager;

import android.app.Activity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;

public final class UIManagerAppCompat {
    private UIManagerAppCompat() {}

    public static void setProgressSpinnerVisible(AppCompatActivity activity, boolean visible) {
        ActionBar bar = activity.getSupportActionBar();

        if (bar.getCustomView() == null) {
            ProgressBar spinner = new ProgressBar(activity);
            spinner.setIndeterminate(true);
            bar.setCustomView(spinner);
        }

        bar.setDisplayShowCustomEnabled(visible);
    }

    public static void setToolbarVisible(AppCompatActivity activity, boolean visible) {
        ActionBar bar = activity.getSupportActionBar();
        if (visible) {
            bar.show();
        } else {
            bar.hide();
        }
    }

    public static boolean isToolbarVisible(AppCompatActivity activity) {
        ActionBar bar = activity.getSupportActionBar();
        return bar.isShowing();
    }

    public static void openOptionsMenu(final AppCompatActivity activity, final View view) {
        if (activity.getSupportActionBar().isShowing()) {
            activity.getSupportActionBar().openOptionsMenu();
        } else {
            view.showContextMenu();
        }
    }

    public static void invalidateOptionsMenu(final AppCompatActivity activity) {
        activity.getSupportActionBar().invalidateOptionsMenu();
    }
}
