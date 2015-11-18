package org.emdev.ui.uimanager;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.res.Configuration;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;

import org.emdev.common.android.AndroidVersion;

import java.util.HashMap;
import java.util.Map;

@TargetApi(14)
public class UIManager40x implements IUIManager {

    protected static final int STANDARD_SYS_UI_FLAGS =
    /**/
    View.SYSTEM_UI_FLAG_LOW_PROFILE |
    /**/
    View.SYSTEM_UI_FLAG_FULLSCREEN |
    /**/
    View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

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
    public void setTitleVisible(final Activity activity, final boolean visible, final boolean firstTime) {
        if (firstTime) {
            try {
                final Window window = activity.getWindow();
                window.requestFeature(Window.FEATURE_ACTION_BAR);
                window.requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
                activity.setProgressBarIndeterminate(true);
                setProgressSpinnerVisible(activity, true);
            } catch (final Throwable th) {
                LCTX.e("Error on requestFeature call: " + th.getMessage());
            }
        }
        try {
            if (visible) {
                activity.getActionBar().show();
            } else {
                activity.getActionBar().hide();
            }
            activity.invalidateOptionsMenu();
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
    public void setProgressSpinnerVisible(Activity activity, boolean visible) {
        try {
            if (AndroidVersion.VERSION >= 21) {
                // Activity.setProgressBarIndeterminateVisibility() seems to be a no-op on API 21+.
                // Instead, set a progress spinner as the action bar's custom view.
                // TODO: Make a UIManager5x?
                ActionBar bar = activity.getActionBar();

                if (bar.getCustomView() == null) {
                    ProgressBar spinner = new ProgressBar(activity);
                    spinner.setIndeterminate(true);
                    bar.setCustomView(spinner);
                }

                bar.setDisplayShowCustomEnabled(visible);
            } else {
                activity.setProgressBarIndeterminateVisibility(visible);
                activity.getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS, visible ? 1 : 0);
            }
        } catch (final Throwable th) {
            LCTX.e("Error in setProgressSpinnerVisible: " + th.getMessage());
        }
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
    public void openOptionsMenu(final Activity activity, final View view) {
        if (data.get(activity.getComponentName()).titleVisible) {
            activity.openOptionsMenu();
        } else {
            view.showContextMenu();
        }
    }

    @Override
    public void invalidateOptionsMenu(final Activity activity) {
        activity.invalidateOptionsMenu();
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
        boolean titleVisible = true;
        boolean statusBarHidden = false;
    }
}
