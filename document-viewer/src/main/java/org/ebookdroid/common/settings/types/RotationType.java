package org.ebookdroid.common.settings.types;

import org.ebookdroid.EBookDroidApp;
import org.sufficientlysecure.viewer.R;

import android.content.pm.ActivityInfo;

import org.emdev.common.android.AndroidVersion;
import org.emdev.utils.enums.ResourceConstant;

public enum RotationType implements ResourceConstant {

    /**
    *
    */
    UNSPECIFIED(R.string.pref_rotation_unspecified, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, 3),
    /**
    *
    */
    LANDSCAPE(R.string.pref_rotation_land, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, 3),
    /**
    *
    */
    PORTRAIT(R.string.pref_rotation_port, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, 3);

    private final String resValue;

    private final int orientation;

    private final int version;

    private RotationType(final int resId, final int orientation, final int version) {
        this.resValue = EBookDroidApp.context.getString(resId);
        this.orientation = orientation;
        this.version = version;
    }

    public String getResValue() {
        return resValue;
    }

    public int getOrientation() {
        return this.version <= AndroidVersion.VERSION ? orientation : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    }

    public int getVersion() {
        return version;
    }
}
