package org.emdev.common.fonts;

import android.graphics.Typeface;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.emdev.BaseDroidApp;
import org.emdev.common.fonts.data.FontFamilyType;
import org.emdev.common.fonts.data.FontInfo;
import org.emdev.utils.LengthUtils;

public class AssetsFontProvider extends AbstractCustomFontProvider {

    public AssetsFontProvider() {
        super(1, "Assets");
    }

    @Override
    protected InputStream openCatalog() throws IOException {
        try {
            return BaseDroidApp.context.getAssets().open("fonts/fonts.jso");
        } catch (Exception ex) {
        }
        return null;
    }

    @Override
    protected Typeface loadTypeface(final FontFamilyType type, final FontInfo fi) {
        final String path = "fonts/" + fi.path;
        try {
            return Typeface.createFromAsset(BaseDroidApp.context.getAssets(), path);
        } catch (final Throwable th) {
            System.err.println("Font loading failed: " + path + ": "
                    + LengthUtils.safeString(th.getMessage(), th.getClass().getName()));
        }
        return null;
    }

    @Override
    protected boolean save() {
        return true;
    }

    @Override
    public InputStream openInputFontStream(FontInfo fi) throws IOException {
        return BaseDroidApp.context.getAssets().open("fonts/" + fi.path);
    }

    @Override
    public OutputStream openOutputFontStream(FontInfo fi) throws IOException {
        return null;
    }
}
