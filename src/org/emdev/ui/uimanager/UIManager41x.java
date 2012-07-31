package org.emdev.ui.uimanager;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
import static android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE;

import android.annotation.TargetApi;

@TargetApi(16)
public class UIManager41x extends UIManager40x {

    private static final int SYS_UI_FLAGS = SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | SYSTEM_UI_FLAG_LAYOUT_STABLE | SYSTEM_UI_FLAG_LOW_PROFILE | SYSTEM_UI_FLAG_FULLSCREEN
            | SYSTEM_UI_FLAG_HIDE_NAVIGATION;

    @Override
    protected int getHideSysUIFlags() {
        return SYS_UI_FLAGS;
    }

}
