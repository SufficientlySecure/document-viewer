package org.ebookdroid.pdfdroid.codec;

import org.ebookdroid.core.BaseViewerActivity;
import org.ebookdroid.core.EBookDroidLibraryLoader;
import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;

import android.util.DisplayMetrics;

import java.lang.reflect.Field;

public class PdfContext extends AbstractCodecContext {

    private static final Field DENSITY_DPI_FIELD;

    static {
        EBookDroidLibraryLoader.load();
        DENSITY_DPI_FIELD = getDensityDPIField();
    }

    private static Field getDensityDPIField() {
        try {
            return DisplayMetrics.class.getDeclaredField("densityDpi");
        } catch (final Throwable ex) {
            return null;
        }
    }

    @Override
    public CodecDocument openDocument(final String fileName, final String password) {
        return new PdfDocument(this, fileName, password);
    }

    public static int getWidthInPixels(final float pdfWidth) {
        return getSizeInPixels(pdfWidth, BaseViewerActivity.DM.xdpi);
    }

    public static int getHeightInPixels(final float pdfHeight) {
        return getSizeInPixels(pdfHeight, BaseViewerActivity.DM.ydpi);
    }

    public static int getSizeInPixels(final float pdfHeight, float dpi) {
        if (dpi == 0) {
            // Archos fix
            dpi = getDensityDPI();
        }
        return (int) (pdfHeight * dpi / 72);
    }

    private static int getDensityDPI() {
        try {
            if (DENSITY_DPI_FIELD != null) {
                return ((Integer) DENSITY_DPI_FIELD.get(BaseViewerActivity.DM)).intValue();
            }
        } catch (final Throwable ex) {
            ex.printStackTrace();
        }
        return 120;
    }
}
