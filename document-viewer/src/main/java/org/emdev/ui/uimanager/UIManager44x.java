package org.emdev.ui.uimanager;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_IMMERSIVE;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE;

import android.annotation.TargetApi;
import android.app.Activity;

@TargetApi(19)
public class UIManager44x extends UIManager40x {

    private static final int EXT_SYS_UI_FLAGS =
    /**/
    SYSTEM_UI_FLAG_LOW_PROFILE |
    /**/
    SYSTEM_UI_FLAG_FULLSCREEN |
    /**/
    SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
    /**/
    SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
    /**/
    SYSTEM_UI_FLAG_HIDE_NAVIGATION |
    /**/
    SYSTEM_UI_FLAG_IMMERSIVE;

    @Override
    protected int getHideSysUIFlags(final Activity activity) {
        return isTabletUi(activity) ? STANDARD_SYS_UI_FLAGS : EXT_SYS_UI_FLAGS;
    }

}
