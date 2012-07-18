package org.emdev.ui.uimanager;


import android.app.Activity;
import android.view.View;

import org.emdev.common.android.AndroidVersion;
import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;

public interface IUIManager {

    LogContext LCTX = LogManager.root().lctx("UIManager");

    IUIManager instance = AndroidVersion.lessThan3x ? new UIManager1x() : AndroidVersion.is3x ? new UIManager3x()
            : new UIManager4x();

    void onPause(Activity activity);

    void onResume(Activity activity);

    void onDestroy(Activity activity);

    void setFullScreenMode(Activity activity, View view, boolean fullScreen);

    void setTitleVisible(Activity activity, boolean visible);

    void setHardwareAccelerationEnabled(Activity activity, boolean enabled);

    void setHardwareAccelerationMode(Activity activity, View view, boolean accelerated);

    void openOptionsMenu(final Activity activity, final View view);

    void onMenuOpened(Activity activity);

    void onMenuClosed(Activity activity);

}
