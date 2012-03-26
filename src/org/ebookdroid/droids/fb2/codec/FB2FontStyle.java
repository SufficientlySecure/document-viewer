package org.ebookdroid.droids.fb2.codec;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.types.FontSize;

import android.util.FloatMath;

public enum FB2FontStyle {

    /**
     * 
     */
    TEXT(1.0f),
    /**
     * 
     */
    MAIN_TITLE(2.0f),
    /**
     * 
     */
    SECTION_TITLE(1.5f),
    /**
     * 
     */
    SUBTITLE(1.25f),
    /**
     * 
     */
    FOOTNOTE(0.84f);

    public static final int TEXT_SIZE = 24;

    public final float factor;

    private FB2FontStyle(final float factor) {
        this.factor = factor;
    }

    int getFontSize() {
        final FontSize fs = SettingsManager.getAppSettings().fontSize;
        return (int) FloatMath.ceil(TEXT_SIZE * fs.factor * this.factor);
    }
}
