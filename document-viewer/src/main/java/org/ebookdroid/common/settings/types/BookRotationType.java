package org.ebookdroid.common.settings.types;

import org.sufficientlysecure.viewer.R;

import org.emdev.BaseDroidApp;
import org.emdev.utils.enums.ResourceConstant;

/**
 * TODO: Replace this enum with RotationType, no need for BookRotationType
 */
public enum BookRotationType implements ResourceConstant {

    /**
    *
    */
    UNSPECIFIED(R.string.pref_rotation_unspecified, RotationType.UNSPECIFIED),
    /**
    *
    */
    LANDSCAPE(R.string.pref_rotation_land, RotationType.LANDSCAPE),
    /**
    *
    */
    PORTRAIT(R.string.pref_rotation_port, RotationType.PORTRAIT),
    /**
     *
     */
    AUTOMATIC(R.string.pref_rotation_auto, RotationType.AUTOMATIC),

    REVERSE_LANDSCAPE(R.string.pref_rotation_reverse_landscape, RotationType.REVERSE_LANDSCAPE),

    REVERSE_PORTRAIT(R.string.pref_rotation_reverse_portrait, RotationType.REVERSE_PORTRAIT);

    private final String resValue;

    private final RotationType orientation;

    private BookRotationType(final int resId, final RotationType orientation) {
        this.resValue = BaseDroidApp.context.getString(resId);
        this.orientation = orientation;
    }

    @Override
    public String getResValue() {
        return resValue;
    }

    public int getOrientation() {
        return orientation.getOrientation();
    }
}
