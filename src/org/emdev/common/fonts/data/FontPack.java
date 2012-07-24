package org.emdev.common.fonts.data;

import java.util.Iterator;

import org.emdev.common.fonts.IFontProvider;
import org.emdev.common.fonts.typeface.TypefaceEx;
import org.emdev.utils.collections.ArrayIterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FontPack implements Iterable<FontFamily> {

    public final IFontProvider provider;

    public final String name;

    protected final FontFamily[] types;

    public FontPack(final IFontProvider provider, final FontPack source) {
        this.provider = provider;
        this.name = source.name;
        this.types = new FontFamily[FontFamilyType.values().length];

        for (final FontFamily ff : source) {
            this.types[ff.type.ordinal()] = ff;
        }
    }

    public FontPack(final IFontProvider manager, final String name, final FontFamily... types) {
        this.provider = manager;
        this.name = name;
        this.types = new FontFamily[FontFamilyType.values().length];

        for (final FontFamily ff : types) {
            this.types[ff.type.ordinal()] = ff;
        }
    }

    public FontPack(final IFontProvider manager, final JSONObject object) throws JSONException {
        this.provider = manager;
        this.name = object.getString("name");
        this.types = new FontFamily[FontFamilyType.values().length];

        JSONArray arr = object.getJSONArray("families");
        for (int i = 0, n = arr.length(); i < n; i++) {
            FontFamily ff = new FontFamily(arr.getJSONObject(i));
            this.types[ff.type.ordinal()] = ff;
        }
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject object = new JSONObject();
        JSONArray arr = new JSONArray();

        object.put("name", name);
        object.put("families", arr);

        for (FontFamily ff : types) {
            if (ff != null) {
                arr.put(ff.toJSON());
            }
        }

        return object;
    }

    @Override
    public Iterator<FontFamily> iterator() {
        return new ArrayIterator<FontFamily>(types);
    }

    public FontFamily getFamily(final FontFamilyType type) {
        return types[type.ordinal()];
    }

    public FontInfo getFont(final FontFamilyType type, final FontStyle style) {
        final FontFamily ff = types[type.ordinal()];
        return ff != null ? ff.fonts[style.ordinal()] : null;
    }

    public TypefaceEx getTypeface(FontFamilyType type, FontStyle style) {
        return provider.getTypeface(this, type, style);
    }

    public String toString() {
        return name;
    }
}
