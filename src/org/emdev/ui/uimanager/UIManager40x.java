package org.emdev.ui.uimanager;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.res.Configuration;
import android.view.View;
import android.view.Window;

import java.util.HashMap;
import java.util.Map;

@TargetApi(14)
public class UIManager40x implements IUIManager {

    protected static final int STANDARD_SYS_UI_FLAGS = View.SYSTEM_UI_FLAG_LOW_PROFILE
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

    protected static final Map<ComponentName, Data> data = new HashMap<ComponentName, Data>() {

        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 742779545837272718L;

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
    public void setTitleVisible(final Activity activity, final boolean visible) {
        try {
            final Window window = activity.getWindow();
            if (!visible) {
                window.requestFeature(Window.FEATURE_NO_TITLE);
                window.requestFeature(Window.FEATURE_ACTION_BAR);
            } else {
                window.requestFeature(Window.FEATURE_ACTION_BAR);
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

    @Override
    public boolean isTitleVisible(final Activity activity) {
        return data.get(activity.getComponentName()).titleVisible;
    }

    @Override
    public void setFullScreenMode(final Activity activity, final View view, final boolean fullScreen) {
        data.get(activity.getComponentName()).statusBarHidden = fullScreen;
        final Window w = activity.getWindow();

        if (!isTabletUi(activity)) {
            if (fullScreen) {
                w.setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);
            } else {
                w.clearFlags(FLAG_FULLSCREEN);
            }
        }

        if (view != null) {
            if (fullScreen) {
                view.setSystemUiVisibility(getHideSysUIFlags(activity));
            } else {
                view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        }
    }

    protected int getHideSysUIFlags(final Activity activity) {
        return STANDARD_SYS_UI_FLAGS;
    }

    @Override
    public void setHardwareAccelerationEnabled(final Activity activity, final boolean enabled) {
        data.get(activity.getComponentName()).hwaEnabled = enabled;
    }

    @Override
    public void setHardwareAccelerationMode(final Activity activity, final View view, final boolean accelerated) {
        if (data.get(activity.getComponentName()).hwaEnabled && view != null) {
            view.setLayerType(accelerated ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    @Override
    public void openOptionsMenu(final Activity activity, final View view) {
        if (data.get(activity.getComponentName()).titleVisible) {
            activity.openOptionsMenu();
        } else {
            view.showContextMenu();
        }
    }

    @Override
    public void onMenuOpened(final Activity activity) {
        if (!isTabletUi(activity)) {
            if (data.get(activity.getComponentName()).statusBarHidden) {
                activity.getWindow().clearFlags(FLAG_FULLSCREEN);
            }
        }
    }

    @Override
    public void onMenuClosed(final Activity activity) {
        if (!isTabletUi(activity)) {
            if (data.get(activity.getComponentName()).statusBarHidden) {
                activity.getWindow().setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);
            }
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
        final Configuration c = activity.getResources().getConfiguration();
        return 0 != (Configuration.SCREENLAYOUT_SIZE_XLARGE & c.screenLayout);
    }

    private static class Data {

        boolean hwaEnabled = false;
        boolean titleVisible = true;
        boolean statusBarHidden = false;
    }
}
