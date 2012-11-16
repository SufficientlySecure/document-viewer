package org.emdev.common.fonts;

import android.graphics.Typeface;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.emdev.common.fonts.data.FontFamily;
import org.emdev.common.fonts.data.FontFamilyType;
import org.emdev.common.fonts.data.FontInfo;
import org.emdev.common.fonts.data.FontPack;
import org.emdev.utils.FileUtils;

public class BaseExtStorageFontProvider extends AbstractCustomFontProvider {

    private final File fontsFolder;

    private final File fontsCatalog;

    public BaseExtStorageFontProvider(final File targetAppStorage) {
        super(2, "External");

        fontsFolder = new File(targetAppStorage, "fonts");
        fontsFolder.mkdirs();
        fontsCatalog = new File(fontsFolder, "fonts.jso");
    }

    @Override
    protected final InputStream openCatalog() throws IOException {
        return fontsCatalog.exists() ? new FileInputStream(fontsCatalog) : null;
    }

    @Override
    protected Typeface loadTypeface(final FontFamilyType type, final FontInfo fi) {
        final File f = getFontFile(fi);
        try {
            return f.exists() ? Typeface.create(type.getResValue(), fi.style.getStyle()) : null;
        } catch (final Throwable th) {
            th.printStackTrace();
        }
        return null;
    }

    public final boolean hasInstalled() {
        return fontsCatalog.exists();
    }

    public final File getFontFile(final FontInfo fi) {
        return fi.path.startsWith("/") ? new File(fi.path) : new File(fontsFolder, fi.path);
    }

    @Override
    protected final boolean save() {
        try {
            final FileWriter fw = new FileWriter(fontsCatalog);
            final BufferedWriter out = new BufferedWriter(fw);
            try {
                out.write(toJSON().toString(4));
                return true;
            } catch (final Exception ex) {
                ex.printStackTrace();
            } finally {
                try {
                    out.close();
                } catch (final IOException ex) {
                }
            }
        } catch (final IOException ex) {
            ex.printStackTrace();
        }

        return false;
    }

    @Override
    public final InputStream openInputFontStream(final FontInfo fi) throws IOException {
        return new FileInputStream(getFontFile(fi));
    }

    @Override
    public final OutputStream openOutputFontStream(final FontInfo fi) throws IOException {
        final File ff = getFontFile(fi);
        ff.getParentFile().mkdirs();
        return new FileOutputStream(ff);
    }

    public final boolean install(final FontPack selectedPack) {
        final AbstractCustomFontProvider source = (AbstractCustomFontProvider) selectedPack.provider;

        final FontPack newfp = new FontPack(this, selectedPack);
        final FontPack oldfp = packs.put(newfp.name, newfp);
        if (oldfp != null) {
            removeImpl(oldfp);
        }

        for (final FontFamily family : newfp) {
            for (final FontInfo fi : family) {
                try {
                    FileUtils.copy(source.openInputFontStream(fi), this.openOutputFontStream(fi));
                } catch (final IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        return this.save();
    }

    public final boolean uninstall(final FontPack fontPack) {
        if (packs.containsKey(fontPack.name)) {
            removeImpl(fontPack);
            return this.save();
        }
        return false;
    }

    private final void removeImpl(final FontPack oldfp) {
        for (final FontFamily family : oldfp) {
            for (final FontInfo fi : family) {
                final File f = getFontFile(fi);
                if (f.exists()) {
                    f.delete();
                }
            }
        }
    }

}
