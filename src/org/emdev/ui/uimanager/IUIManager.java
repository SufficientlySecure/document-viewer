package org.emdev.ui.uimanager;

import org.ebookdroid.common.log.LogContext;

import android.app.Activity;
import android.view.View;

import org.emdev.utils.android.AndroidVersion;

public interface IUIManager {

    LogContext LCTX = LogContext.ROOT.lctx("UIManager");

    IUIManager instance = AndroidVersion.lessThan3x ? new UIManager1x() : AndroidVersion.is3x ? new UIManager3x()
            : new UIManager4x();

    void onPause(Activity activity);

    void onResume(Activity activity);

    void onDestroy(Activity activity);

    void setFullScreenMode(Activity activity, View view, boolean fullScreen);

    void setTitleVisible(Activity activity, boolean visible);

    void setHardwareAccelerationEnabled(boolean enabled);

    void setHardwareAccelerationMode(View view, boolean accelerated);

    void openOptionsMenu(final Activity activity, final View view);

    void onMenuOpened(Activity activity);

    void onMenuClosed(Activity activity);

}
