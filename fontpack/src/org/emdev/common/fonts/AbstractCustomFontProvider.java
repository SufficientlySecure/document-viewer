package org.emdev.common.fonts;

import android.graphics.Typeface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.emdev.common.fonts.data.FontFamilyType;
import org.emdev.common.fonts.data.FontInfo;
import org.emdev.common.fonts.data.FontPack;
import org.emdev.common.fonts.data.FontStyle;
import org.emdev.common.fonts.typeface.TypefaceEx;
import org.json.JSONArray;
import org.json.JSONException;

public abstract class AbstractCustomFontProvider extends AbstractFontProvider {

    protected AbstractCustomFontProvider(int id, String name) {
        super(id, name);
    }

    @Override
    public TypefaceEx getTypeface(final FontPack fp, final FontFamilyType type, final FontStyle style) {
        FontInfo font = fp.getFont(type, style);
        Typeface target = null;
        if (font != null) {
            target = loadTypeface(type, font);
        }
        if (target == null) {
            final FontStyle base = style.getBase();
            if (base != style) {
                font = fp.getFont(type, base);
                if (font != null) {
                    target = loadTypeface(type, font);
                }
            }
        }
        if (target == null) {
            return null;
        }

        final int st = style.getStyle();
        final boolean fake = (st & Typeface.BOLD) != (target.getStyle() & Typeface.BOLD);
        return new TypefaceEx(fp, type, style, target, fake);
    }

    @Override
    protected List<FontPack> load() {
        try {
            return loadCatalog();
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
        return Collections.emptyList();
    }

    protected List<FontPack> loadCatalog() throws IOException, JSONException {
        final List<FontPack> list = new ArrayList<FontPack>();

        final InputStream stream = openCatalog();
        if (stream != null) {
            final BufferedReader in = new BufferedReader(new InputStreamReader(stream));
            try {
                final StringBuilder buf = new StringBuilder();
                for (String s = in.readLine(); s != null; s = in.readLine()) {
                    buf.append(s).append("\n");
                }
                final JSONArray arr = new JSONArray(buf.toString());
                for (int i = 0, n = arr.length(); i < n; i++) {
                    list.add(new FontPack(this, arr.getJSONObject(i)));
                }
            } finally {
                try {
                    in.close();
                } catch (final Exception ex) {
                }
            }
        }
        return list;
    }

    protected final JSONArray fromJSON(final InputStream stream) throws IOException, JSONException {
        final StringBuilder buf = new StringBuilder();
        final BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        for (String s = in.readLine(); s != null; s = in.readLine()) {
            buf.append(s).append("\n");
        }
        return new JSONArray(buf.toString());
    }

    protected final JSONArray toJSON() throws JSONException {
        JSONArray arr = new JSONArray();

        for (FontPack fp : packs.values()) {
            arr.put(fp.toJSON());
        }

        return arr;
    }

    protected abstract InputStream openCatalog() throws IOException;

    protected abstract Typeface loadTypeface(final FontFamilyType type, FontInfo fi);

    public abstract InputStream openInputFontStream(FontInfo fi) throws IOException;

    public abstract OutputStream openOutputFontStream(FontInfo fi) throws IOException;

}
