package org.emdev.common.fonts.typeface;

import android.graphics.Typeface;

import org.emdev.common.fonts.data.FontFamilyType;
import org.emdev.common.fonts.data.FontPack;
import org.emdev.common.fonts.data.FontStyle;

public class TypefaceEx {

    public final int id;
    public final FontPack pack;
    public final FontFamilyType family;
    public final FontStyle style;
    public final Typeface typeface;
    public final boolean fakeBold;

    public TypefaceEx(final FontPack pack, final FontFamilyType family, final FontStyle style, final Typeface typeface,
            final boolean fakeBold) {
        this.id = calculateId(pack, family, style);
        this.pack = pack;
        this.family = family;
        this.style = style;
        this.typeface = typeface;
        this.fakeBold = fakeBold;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder(this.getClass().getSimpleName());
        buf.append("[");

        buf.append(pack.provider);
        buf.append(" ");
        buf.append(pack.name);
        buf.append(" ");
        buf.append(family.getResValue());
        buf.append(" ");
        buf.append(style.getResValue());
        buf.append(", ");
        buf.append("fakeBold").append("=").append(fakeBold);
        buf.append(", ");
        buf.append("tf").append("=").append(typeface);

        buf.append("]");

        return buf.toString();
    }

    public static int calculateId(final FontPack pack, final FontFamilyType family, final FontStyle style) {
        return (pack.id << 8) + (pack.provider.getId() << 6) + (family.ordinal() << 3) + (style.ordinal() << 1);
    }
}
