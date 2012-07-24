package org.emdev.common.fonts;

import org.ebookdroid.EBookDroidApp;

import java.io.File;

import org.emdev.common.fonts.data.FontFamilyType;
import org.emdev.common.fonts.data.FontInfo;
import org.emdev.common.fonts.data.FontPack;
import org.emdev.common.fonts.data.FontStyle;

public class FontManager {

    public static final SystemFontProvider system = new SystemFontProvider();
    public static final AssetsFontProvider assets = new AssetsFontProvider();
    public static final ExtStorageFontProvider external = new ExtStorageFontProvider(EBookDroidApp.APP_STORAGE);

    public static void init() {
        system.init();
        assets.init();
        external.init();
    }

    public static String getExternalFont(final String fontPackName, final FontFamilyType type, final FontStyle style) {
        final FontPack fontPack = external.getFontPack(fontPackName);
        if (fontPack == null) {
            return null;
        }
        final FontInfo font = fontPack.getFont(type, style);
        if (font == null) {
            return null;
        }
        final File fontFile = external.getFontFile(font);
        if (fontFile.exists()) {
            return fontFile.getAbsolutePath();
        }
        return null;
    }

    public static String[] getExternalFonts(final String fontPackName, final FontFamilyType type) {
        final FontStyle[] styles = FontStyle.values();
        final String[] fonts = new String[styles.length];
        for (int i = 0; i < styles.length; i++) {
            fonts[i] = getExternalFont(fontPackName, type, styles[i]);
        }
        return fonts;
    }
}
