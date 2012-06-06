package org.ebookdroid.droids.mupdf.codec;

import org.ebookdroid.EBookDroidLibraryLoader;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;

import android.graphics.Bitmap;

public class MuPdfContext extends AbstractCodecContext {


    public static final Bitmap.Config BITMAP_CFG = Bitmap.Config.RGB_565;

    public static final Bitmap.Config NATIVE_BITMAP_CFG = Bitmap.Config.ARGB_8888;

    static {
        EBookDroidLibraryLoader.load();
    }

    @Override
    public Bitmap.Config getBitmapConfig() {
        return EBookDroidLibraryLoader.nativeGraphicsAvailable && AppSettings.current().useNativeGraphics ? NATIVE_BITMAP_CFG : BITMAP_CFG;
    }


    @Override
    public CodecDocument openDocument(String fileName, String password) {
        return null;
    }
    
    private static native boolean isNativeGraphicsAvailable();

}
