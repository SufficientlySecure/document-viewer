package org.ebookdroid.core;

import android.content.pm.ActivityInfo;

public enum RotationType {
    /**
     *
     */
    AUTOMATIC("Automatic", ActivityInfo.SCREEN_ORIENTATION_SENSOR),
    /**
     *
     */
    LANDSCAPE("Force landscape", ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE),
    /**
     *
     */
    PORTRAIT("Force portrait", ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

    private final String resValue;

    private final int orientation;

    private RotationType(final String resValue, final int orientation) {
        this.resValue = resValue;
        this.orientation = orientation;
    }

    public String getResValue() {
        return resValue;
    }

    public int getOrientation() {
        return orientation;
    }

    public static RotationType getByResValue(final String rotationStr) {
        for (final RotationType rt : values()) {
            if (rt.getResValue().equalsIgnoreCase(rotationStr)) {
                return rt;
            }
        }
        return null;
    }

}
