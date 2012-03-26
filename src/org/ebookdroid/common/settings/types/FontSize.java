package org.ebookdroid.common.settings.types;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.R;

import org.emdev.utils.enums.ResourceConstant;

public enum FontSize implements ResourceConstant {
    /**
     * 
     */
    TINY(R.string.pref_fontsize_tiny, 0.5f),
    /**
     * 
     */
    SMALL(R.string.pref_fontsize_small, 0.707f),
    /**
     * 
     */
    NORMAL(R.string.pref_fontsize_normal, 1.0f),
    /**
     * 
     */
    LARGE(R.string.pref_fontsize_large, 1.414f),
    /**
     * 
     */
    HUGE(R.string.pref_fontsize_huge, 2.0f);

    public final float factor;

    private final String resValue;
    
    private FontSize(final int resId, final float factor) {
        this.resValue = EBookDroidApp.context.getString(resId);
        this.factor = factor;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.utils.enums.ResourceConstant#getResValue()
     */
    @Override
    public String getResValue() {
        return resValue;
    }
}
