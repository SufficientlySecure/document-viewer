package org.emdev.common.fonts;

import android.graphics.Typeface;

import java.util.Arrays;
import java.util.List;

import org.emdev.common.fonts.data.FontFamily;
import org.emdev.common.fonts.data.FontFamilyType;
import org.emdev.common.fonts.data.FontInfo;
import org.emdev.common.fonts.data.FontPack;
import org.emdev.common.fonts.data.FontStyle;
import org.emdev.common.fonts.typeface.TypefaceEx;

public class SystemFontProvider extends AbstractFontProvider {

    public static final String SYSTEM_FONT_PACK = "System";

    public SystemFontProvider() {
        super(0, SYSTEM_FONT_PACK);
    }

    @Override
    protected List<FontPack> load() {
        return Arrays.asList((FontPack) new SystemFontPack(this, SYSTEM_FONT_PACK));
    }

    @Override
    public TypefaceEx getTypeface(final FontPack fp, final FontFamilyType type, final FontStyle style) {
        final int st = style.getStyle();
        final Typeface family = type.getSystem();
        final Typeface target = Typeface.create(family, st);
        final boolean fake = (st & Typeface.BOLD) != (target.getStyle() & Typeface.BOLD);
        return new TypefaceEx(fp, type, style, target, fake);
    }

    private static class SystemFontPack extends FontPack {

        public SystemFontPack(SystemFontProvider manager, final String name) {
            super(manager, name);
            for (final FontFamilyType type : FontFamilyType.values()) {
                final FontFamily ff = new SystemFontFamily(type);
                this.types[type.ordinal()] = ff;
            }
        }
    }

    private static class SystemFontFamily extends FontFamily {

        public SystemFontFamily(final FontFamilyType type) {
            super(type);
            for (final FontStyle fs : FontStyle.values()) {
                final FontInfo fi = new FontInfo("", fs);
                this.fonts[fs.ordinal()] = fi;
            }
        }
    }
}
