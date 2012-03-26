package org.ebookdroid.common.settings.types;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.R;

import android.content.pm.ActivityInfo;

import org.emdev.utils.enums.ResourceConstant;

public enum RotationType implements ResourceConstant {
    /**
     *
     */
    AUTOMATIC(R.string.pref_rotation_auto, ActivityInfo.SCREEN_ORIENTATION_SENSOR),
    /**
     *
     */
    LANDSCAPE(R.string.pref_rotation_land, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE),
    /**
     *
     */
    PORTRAIT(R.string.pref_rotation_port, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

    private final String resValue;

    private final int orientation;

    private RotationType(final int resId, final int orientation) {
        this.resValue = EBookDroidApp.context.getString(resId);
        this.orientation = orientation;
    }

    public String getResValue() {
        return resValue;
    }

    public int getOrientation() {
        return orientation;
    }
}
