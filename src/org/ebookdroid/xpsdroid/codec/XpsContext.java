package org.ebookdroid.xpsdroid.codec;

import org.ebookdroid.core.EBookDroidLibraryLoader;
import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;

import android.graphics.Bitmap;

public class XpsContext extends  AbstractCodecContext {

    public static final boolean useNativeGraphics;

    public static final Bitmap.Config BITMAP_CFG = Bitmap.Config.RGB_565;

    public static final Bitmap.Config NATIVE_BITMAP_CFG = Bitmap.Config.ARGB_8888;

    static {
        EBookDroidLibraryLoader.load();
        useNativeGraphics = isNativeGraphicsAvailable();
    }

    @Override
    public Bitmap.Config getBitmapConfig() {
        return useNativeGraphics ? NATIVE_BITMAP_CFG : BITMAP_CFG;
    }

    @Override
    public CodecDocument openDocument(final String fileName, final String password) {
        return new XpsDocument(this, fileName);
    }

    private static native boolean isNativeGraphicsAvailable();
}
