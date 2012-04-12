package org.ebookdroid.droids.xps.codec;

import org.ebookdroid.EBookDroidLibraryLoader;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;

import android.graphics.Bitmap;

public class XpsContext extends AbstractCodecContext {

    public static final Bitmap.Config BITMAP_CFG = Bitmap.Config.RGB_565;

    public static final Bitmap.Config NATIVE_BITMAP_CFG = Bitmap.Config.ARGB_8888;

    static {
        EBookDroidLibraryLoader.load();
    }

    @Override
    public Bitmap.Config getBitmapConfig() {
        return EBookDroidLibraryLoader.nativeGraphicsAvailable && SettingsManager.getAppSettings().useNativeGraphics ? NATIVE_BITMAP_CFG
                : BITMAP_CFG;
    }

    @Override
    public CodecDocument openDocument(final String fileName, final String password) {
        return new XpsDocument(this, fileName);
    }
}
