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

        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = -3701577210751612032L;

        @Override
        public Data get(final Object key) {
            Data existing = super.get(key);
            if (existing == null) {
                existing = new Data();
                put((ComponentName) key, existing);
            }
            return existing;
        }

    };

    @Override
    public void setTitleVisible(final Activity activity, final boolean visible, final boolean firstTime) {
        if (firstTime) {
            try {
                final Window window = activity.getWindow();
                if (!visible) {
                    window.requestFeature(Window.FEATURE_NO_TITLE);
                } else {
                    window.requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
                    activity.setProgressBarIndeterminate(true);
                    activity.setProgressBarIndeterminateVisibility(true);
                    window.setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS, 1);
                }
                data.get(activity.getComponentName()).titleVisible = visible;
            } catch (final Throwable th) {
                LCTX.e("Error on requestFeature call: " + th.getMessage());
            }
        }
    }

    @Override
    public boolean isTitleVisible(final Activity activity) {
        return data.get(activity.getComponentName()).titleVisible;
    }

    @Override
    public void setFullScreenMode(final Activity activity, final View view, final boolean fullScreen) {
        data.get(activity.getComponentName()).fullScreen = fullScreen;
        final Window w = activity.getWindow();
        if (fullScreen) {
            w.setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);
        } else {
            w.clearFlags(FLAG_FULLSCREEN);
        }
    }

    @Override
    public void openOptionsMenu(final Activity activity, final View view) {
        activity.openOptionsMenu();
    }

    @Override
    public void invalidateOptionsMenu(final Activity activity) {
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

    @Override
    public boolean isTabletUi(final Activity activity) {
        return false;
    }

    private static class Data {

        boolean fullScreen = false;
        boolean titleVisible = true;
    }

}
