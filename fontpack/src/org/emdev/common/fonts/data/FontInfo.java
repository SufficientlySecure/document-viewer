package org.emdev.common.fonts.data;

public class FontInfo {

    public final String path;

    public final FontStyle style;

    public FontInfo(final String path, final FontStyle style) {
        this.path = path;
        this.style = style;
    }

    @Override
    public String toString() {
        return style.getResValue() + ": " + path;
    }
}
