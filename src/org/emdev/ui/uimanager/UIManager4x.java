package org.emdev.ui.uimanager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Configuration;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

@TargetApi(14)
public class UIManager4x implements IUIManager {

    private static final int FLAG_FULLSCREEN = WindowManager.LayoutParams.FLAG_FULLSCREEN;

    private boolean hwaEnabled = false;

    private boolean titleVisible;

    private boolean statusBarHidden = false;

    @Override
    public void setTitleVisible(final Activity activity, final boolean visible) {
        this.titleVisible = visible;
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
        } catch (final Throwable th) {
            LCTX.e("Error on requestFeature call: " + th.getMessage());
        }
    }

    @Override
    public void setFullScreenMode(final Activity activity, final View view, final boolean fullScreen) {
        this.statusBarHidden = fullScreen;
        if (!isTabletUi(activity)) {
            final Window w = activity.getWindow();
            if (fullScreen) {
                w.setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);
            } else {
                w.clearFlags(FLAG_FULLSCREEN);
            }
        } else {
            if (view != null) {
                if (fullScreen) {
                    view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                } else {
                    view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                }
            }
        }
    }

    @Override
    public void setHardwareAccelerationEnabled(final boolean enabled) {
        this.hwaEnabled = enabled;
    }

    @Override
    public void setHardwareAccelerationMode(final View view, final boolean accelerated) {
        if (this.hwaEnabled && view != null) {
            view.setLayerType(accelerated ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    @Override
    public void openOptionsMenu(final Activity activity, final View view) {
        if (titleVisible) {
            activity.openOptionsMenu();
        } else {
            view.showContextMenu();
        }
    }

    @Override
    public void onMenuOpened(final Activity activity) {
        if (!isTabletUi(activity)) {
            if (statusBarHidden) {
                activity.getWindow().clearFlags(FLAG_FULLSCREEN);
            }
        }
    }

    @Override
    public void onMenuClosed(final Activity activity) {
        if (!isTabletUi(activity)) {
            if (statusBarHidden) {
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

    private boolean isTabletUi(final Activity activity) {
        final Configuration c = activity.getResources().getConfiguration();
        return 0 != (Configuration.SCREENLAYOUT_SIZE_XLARGE & c.screenLayout);
    }
}
