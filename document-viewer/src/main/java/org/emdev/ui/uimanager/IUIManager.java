package org.emdev.ui.uimanager;

import android.app.Activity;
import android.view.View;

import org.emdev.common.android.AndroidVersion;
import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;

public interface IUIManager {

    LogContext LCTX = LogManager.root().lctx("UIManager");

    IUIManager instance =
    /* Check old versions */
    AndroidVersion.lessThan3x
    /* UIManager1x */
    ? new UIManager1x()
    /* Check Android 3.x versions */
    : AndroidVersion.is3x
    /* UIManager3x */
    ? new UIManager3x()
    /* Check Android 4.0.x versions */
    : AndroidVersion.is40x
    /* UIManager40x */
    ? new UIManager40x()
    /* Check Android 4.0.x versions */
    : AndroidVersion.is41x || AndroidVersion.is42x || AndroidVersion.is43x
    /* UIManager41x */
    ? new UIManager41x()
    /* UIManager44x */
    : new UIManager44x();

    void onPause(Activity activity);

    void onResume(Activity activity);

    void onDestroy(Activity activity);

    void setFullScreenMode(Activity activity, View view, boolean fullScreen);

    void onMenuOpened(Activity activity);

    void onMenuClosed(Activity activity);

    boolean isTabletUi(final Activity activity);
}
