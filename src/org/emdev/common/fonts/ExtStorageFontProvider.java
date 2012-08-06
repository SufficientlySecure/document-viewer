package org.emdev.common.fonts;

import android.graphics.Typeface;

import java.io.File;

import org.emdev.common.fonts.data.FontFamilyType;
import org.emdev.common.fonts.data.FontInfo;

public class ExtStorageFontProvider extends BaseExtStorageFontProvider {

    public ExtStorageFontProvider(final File targetAppStorage) {
        super(targetAppStorage);
    }

    @Override
    protected Typeface loadTypeface(final FontFamilyType type, final FontInfo fi) {
        final File f = getFontFile(fi);
        try {
            return f.exists() ? Typeface.createFromFile(f) : null;
        } catch (final Throwable th) {
            th.printStackTrace();
        }
        return null;
    }
}
