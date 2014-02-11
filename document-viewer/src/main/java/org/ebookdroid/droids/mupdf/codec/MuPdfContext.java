package org.ebookdroid.droids.mupdf.codec;

import org.ebookdroid.EBookDroidLibraryLoader;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.core.codec.AbstractCodecContext;

import java.util.Arrays;

import org.emdev.common.fonts.FontManager;
import org.emdev.common.fonts.data.FontFamilyType;
import org.emdev.common.fonts.data.FontStyle;
import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;

public abstract class MuPdfContext extends AbstractCodecContext {

    public static final LogContext LCTX = LogManager.root().lctx("MuPdf");

    public static final int MUPDF_FEATURES = FEATURE_CACHABLE_PAGE_INFO | FEATURE_EMBEDDED_OUTLINE
            | FEATURE_PAGE_TEXT_SEARCH | FEATURE_POSITIVE_IMAGES_IN_NIGHT_MODE | FEATURE_CROP_SUPPORT
            | FEATURE_SPLIT_SUPPORT;

    static {
        EBookDroidLibraryLoader.load();
    }

    public MuPdfContext() {
        super(MUPDF_FEATURES);
    }

    public void setExternalFonts() {
        final AppSettings app = AppSettings.current();

        final String[] monoFonts = FontManager.getExternalFonts(app.monoFontPack, FontFamilyType.MONO);
        LCTX.d("Mono fonts: " + Arrays.toString(monoFonts));

        setMonoFonts(monoFonts[FontStyle.REGULAR.ordinal()], monoFonts[FontStyle.ITALIC.ordinal()],
                monoFonts[FontStyle.BOLD.ordinal()], monoFonts[FontStyle.BOLD_ITALIC.ordinal()]);

        final String[] sansFonts = FontManager.getExternalFonts(app.sansFontPack, FontFamilyType.SANS);
        LCTX.d("Sans fonts: " + Arrays.toString(sansFonts));

        setSansFonts(sansFonts[FontStyle.REGULAR.ordinal()], sansFonts[FontStyle.ITALIC.ordinal()],
                sansFonts[FontStyle.BOLD.ordinal()], sansFonts[FontStyle.BOLD_ITALIC.ordinal()]);

        final String[] serifFonts = FontManager.getExternalFonts(app.serifFontPack, FontFamilyType.SERIF);
        LCTX.d("Serif fonts: " + Arrays.toString(serifFonts));

        setSerifFonts(serifFonts[FontStyle.REGULAR.ordinal()], serifFonts[FontStyle.ITALIC.ordinal()],
                serifFonts[FontStyle.BOLD.ordinal()], serifFonts[FontStyle.BOLD_ITALIC.ordinal()]);

        final String symbolFont = FontManager.getExternalFont(app.symbolFontPack, FontFamilyType.SYMBOL,
                FontStyle.REGULAR);
        LCTX.d("Symbol font: " + symbolFont);
        setSymbolFont(symbolFont);

        final String dingbatFont = FontManager.getExternalFont(app.dingbatFontPack, FontFamilyType.DINGBAT,
                FontStyle.REGULAR);
        LCTX.d("Dingbat font: " + dingbatFont);
        setDingbatFont(dingbatFont);
    }

    private static native void setMonoFonts(String regular, String italic, String bold, String boldItalic);

    private static native void setSansFonts(String regular, String italic, String bold, String boldItalic);

    private static native void setSerifFonts(String regular, String italic, String bold, String boldItalic);

    private static native void setSymbolFont(String regular);

    private static native void setDingbatFont(String regular);
}
