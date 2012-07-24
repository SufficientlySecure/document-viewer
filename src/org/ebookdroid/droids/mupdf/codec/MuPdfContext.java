package org.ebookdroid.droids.mupdf.codec;

import org.ebookdroid.EBookDroidLibraryLoader;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;

import android.graphics.Bitmap;

import org.emdev.common.fonts.FontManager;
import org.emdev.common.fonts.data.FontFamilyType;
import org.emdev.common.fonts.data.FontStyle;

public class MuPdfContext extends AbstractCodecContext {

    public static final Bitmap.Config BITMAP_CFG = Bitmap.Config.RGB_565;

    public static final Bitmap.Config NATIVE_BITMAP_CFG = Bitmap.Config.ARGB_8888;

    static {
        EBookDroidLibraryLoader.load();
    }

    public MuPdfContext() {
        final AppSettings app = AppSettings.current();

        final String[] monoFonts = FontManager.getExternalFonts(app.monoFontPack, FontFamilyType.MONO);
        setMonoFonts(monoFonts[FontStyle.REGULAR.ordinal()], monoFonts[FontStyle.ITALIC.ordinal()],
                monoFonts[FontStyle.BOLD.ordinal()], monoFonts[FontStyle.BOLD_ITALIC.ordinal()]);

        final String[] sansFonts = FontManager.getExternalFonts(app.sansFontPack, FontFamilyType.SANS);
        setSansFonts(sansFonts[FontStyle.REGULAR.ordinal()], sansFonts[FontStyle.ITALIC.ordinal()],
                sansFonts[FontStyle.BOLD.ordinal()], sansFonts[FontStyle.BOLD_ITALIC.ordinal()]);

        final String[] serifFonts = FontManager.getExternalFonts(app.serifFontPack, FontFamilyType.SERIF);
        setSerifFonts(serifFonts[FontStyle.REGULAR.ordinal()], serifFonts[FontStyle.ITALIC.ordinal()],
                serifFonts[FontStyle.BOLD.ordinal()], serifFonts[FontStyle.BOLD_ITALIC.ordinal()]);

        setSymbolFont(FontManager.getExternalFont(app.symbolFontPack, FontFamilyType.SYMBOL, FontStyle.REGULAR));
        setDingbatFont(FontManager.getExternalFont(app.dingbatFontPack, FontFamilyType.DINGBAT, FontStyle.REGULAR));
    }

    @Override
    public Bitmap.Config getBitmapConfig() {
        return EBookDroidLibraryLoader.nativeGraphicsAvailable && AppSettings.current().useNativeGraphics ? NATIVE_BITMAP_CFG
                : BITMAP_CFG;
    }

    @Override
    public CodecDocument openDocument(final String fileName, final String password) {
        return null;
    }

    @Override
    public boolean isParallelPageAccessAvailable() {
        return false;
    }

    private static native void setMonoFonts(String regular, String italic, String bold, String boldItalic);

    private static native void setSansFonts(String regular, String italic, String bold, String boldItalic);

    private static native void setSerifFonts(String regular, String italic, String bold, String boldItalic);

    private static native void setSymbolFont(String regular);

    private static native void setDingbatFont(String regular);
}
