package org.ebookdroid.common.settings.types;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.R;

import org.emdev.utils.enums.ResourceConstant;

/**
 * The Enum PageAlign.
 */
public enum PageAlign implements ResourceConstant {
    /** BY WIDTH. */
    WIDTH(R.string.pref_align_by_width),
    /** BY HEIGHT. */
    HEIGHT(R.string.pref_align_by_height),
    /** AUTO. */
    AUTO(R.string.pref_align_auto);

    /** The resource value. */
    private final String resValue;

    /**
     * Instantiates a new page align object.
     *
     * @param resValue
     *            the res value
     */
    private PageAlign(final int resId) {
        this.resValue = EBookDroidApp.context.getString(resId);
    }

    /**
     * Gets the resource value.
     *
     * @return the resource value
     */
    public String getResValue() {
        return resValue;
    }
}
