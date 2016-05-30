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

    private static native void setMonoFonts(String regular, String italic, String bold, String boldItalic);

    private static native void setSansFonts(String regular, String italic, String bold, String boldItalic);

    private static native void setSerifFonts(String regular, String italic, String bold, String boldItalic);

    private static native void setSymbolFont(String regular);

    private static native void setDingbatFont(String regular);
}
