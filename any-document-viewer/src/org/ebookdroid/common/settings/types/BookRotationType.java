package org.ebookdroid.common.settings.types;

import org.ebookdroid.R;

import org.emdev.BaseDroidApp;
import org.emdev.utils.enums.ResourceConstant;

public enum BookRotationType implements ResourceConstant {

    /**
    *
    */
    UNSPECIFIED(R.string.pref_rotation_unspecified, null),
    /**
    *
    */
    LANDSCAPE(R.string.pref_rotation_land, RotationType.LANDSCAPE),
    /**
    *
    */
    PORTRAIT(R.string.pref_rotation_port, RotationType.PORTRAIT);

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

    public int getOrientation(final RotationType defRotation) {
        return orientation != null ? orientation.getOrientation() : defRotation.getOrientation();
    }
}
