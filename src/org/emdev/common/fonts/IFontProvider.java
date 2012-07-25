package org.emdev.common.fonts;

import org.emdev.common.fonts.data.FontFamilyType;
import org.emdev.common.fonts.data.FontPack;
import org.emdev.common.fonts.data.FontStyle;
import org.emdev.common.fonts.typeface.TypefaceEx;

public interface IFontProvider extends Iterable<FontPack> {

    int getId();

    int getNewPackId();

    String getName();

    FontPack getFontPack(final String name);

    TypefaceEx getTypeface(FontPack fp, FontFamilyType type, FontStyle style);

}
