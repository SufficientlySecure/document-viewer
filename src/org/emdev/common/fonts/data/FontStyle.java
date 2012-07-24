package org.emdev.common.fonts.data;

import android.graphics.Typeface;

import org.emdev.utils.enums.EnumUtils;
import org.emdev.utils.enums.ResourceConstant;

public enum FontStyle implements ResourceConstant {

    /**
     *
     */
    REGULAR("regular", "regular", Typeface.NORMAL),
    /**
     *
     */
    ITALIC("italic", "regular", Typeface.ITALIC),
    /**
     *
     */
    BOLD("bold", "regular", Typeface.BOLD),
    /**
     *
     */
    BOLD_ITALIC("bold italic", "italic", Typeface.BOLD_ITALIC);

    private final String value;
    private final String base;
    private final int style;

    private FontStyle(final String value, final String base, final int style) {
        this.value = value;
        this.base = base;
        this.style = style;
    }

    @Override
    public String getResValue() {
        return value;
    }

    public FontStyle getBase() {
        return EnumUtils.getByResValue(FontStyle.class, base, FontStyle.REGULAR);
    }

    public int getStyle() {
        return style;
    }
}
