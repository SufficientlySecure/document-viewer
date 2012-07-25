package org.emdev.common.fonts.data;

import android.graphics.Typeface;

import org.emdev.utils.enums.EnumUtils;
import org.emdev.utils.enums.ResourceConstant;

public enum FontStyle implements ResourceConstant {

    /**
     *
     */
    REGULAR("regular", "regular", "italic", "bold", Typeface.NORMAL),
    /**
     *
     */
    ITALIC("italic", "regular", "italic", "bold italic", Typeface.ITALIC),
    /**
     *
     */
    BOLD("bold", "regular", "bold italic", "bold", Typeface.BOLD),
    /**
     *
     */
    BOLD_ITALIC("bold italic", "italic", "bold italic", "bold italic", Typeface.BOLD_ITALIC);

    private final String value;
    private final String base;
    private final String italic;
    private final String bold;
    private final int style;

    private FontStyle(final String value, final String base, final String italic, final String bold, final int style) {
        this.value = value;
        this.base = base;
        this.italic = italic;
        this.bold = bold;
        this.style = style;
    }

    @Override
    public String getResValue() {
        return value;
    }

    public FontStyle getBase() {
        return EnumUtils.getByResValue(FontStyle.class, base, FontStyle.REGULAR);
    }

    public FontStyle getBold() {
        return EnumUtils.getByResValue(FontStyle.class, bold, FontStyle.REGULAR);
    }

    public FontStyle getItalic() {
        return EnumUtils.getByResValue(FontStyle.class, italic, FontStyle.REGULAR);
    }

    public int getStyle() {
        return style;
    }
}
