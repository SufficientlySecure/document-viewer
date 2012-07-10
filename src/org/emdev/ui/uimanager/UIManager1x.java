package org.emdev.ui.uimanager;

import android.app.Activity;
import android.content.ComponentName;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.HashMap;
import java.util.Map;

public class UIManager1x implements IUIManager {

    private static final int FLAG_FULLSCREEN = WindowManager.LayoutParams.FLAG_FULLSCREEN;

    private static final Map<ComponentName, Data> data = new HashMap<ComponentName, Data>() {

        @Override
        public Data get(Object key) {
            Data existing = super.get(key);
            if (existing == null) {
                existing = new Data();
                put((ComponentName) key, existing);
            }
            return existing;
        }

    };

    @Override
    public void setTitleVisible(Activity activity, boolean visible) {
        try {
            Window window = activity.getWindow();
            if (!visible) {
                window.requestFeature(Window.FEATURE_NO_TITLE);
            } else {
                window.requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
                activity.setProgressBarIndeterminate(true);
                activity.setProgressBarIndeterminateVisibility(true);
                window.setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS, 1);
            }
        } catch (final Throwable th) {
            LCTX.e("Error on requestFeature call: " + th.getMessage());
        }
    }

    @Override
    public void setFullScreenMode(final Activity activity, final View view, final boolean fullScreen) {
        data.get(activity.getComponentName()).fullScreen = fullScreen;
        Window w = activity.getWindow();
        if (fullScreen) {
            w.setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);
        } else {
            w.clearFlags(FLAG_FULLSCREEN);
        }
    }

    @Override
    public void setHardwareAccelerationEnabled(final Activity activity, final boolean enabled) {
    }

    @Override
    public void setHardwareAccelerationMode(final Activity activity, final View view, final boolean accelerated) {
    }

    @Override
    public void openOptionsMenu(final Activity activity, final View view) {
        activity.openOptionsMenu();
    }

    @Override
    public void onMenuOpened(final Activity activity) {
        if (data.get(activity.getComponentName()).fullScreen) {
            activity.getWindow().clearFlags(FLAG_FULLSCREEN);
        }
    }

    @Override
    public void onMenuClosed(final Activity activity) {
        if (data.get(activity.getComponentName()).fullScreen) {
            activity.getWindow().setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);
        }
    }

    @Override
    public void onPause(final Activity activity) {
    }

    @Override
    public void onResume(final Activity activity) {
    }

    @Override
    public void onDestroy(final Activity activity) {
    }

    private static class Data {
        boolean fullScreen = false;
    }
}
