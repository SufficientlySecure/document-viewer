package org.emdev.utils.textmarkup;

import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.types.FontSize;

import android.util.FloatMath;

public enum FontStyle {

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

    private FontStyle(final float factor) {
        this.factor = factor;
    }

    public int getFontSize() {
        final FontSize fs = AppSettings.current().fontSize;
        return (int) FloatMath.ceil(TEXT_SIZE * fs.factor * this.factor);
    }
}
