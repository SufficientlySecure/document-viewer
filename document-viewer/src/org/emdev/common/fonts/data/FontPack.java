package org.emdev.common.fonts.data;

import java.util.Iterator;

import org.emdev.common.fonts.IFontProvider;
import org.emdev.common.fonts.typeface.TypefaceEx;
import org.emdev.utils.collections.ArrayIterator;
import org.json.JSONException;
import org.json.JSONObject;

public class FontPack implements Iterable<FontFamily> {

    public final IFontProvider provider;

    public final int id;

    public final String name;

    protected final FontFamily[] types;

    public FontPack(final IFontProvider provider, final FontPack source) {
        this.id = provider.getNewPackId();
        this.provider = provider;
        this.name = source.name;
        this.types = new FontFamily[FontFamilyType.values().length];

        for (final FontFamily ff : source) {
            this.types[ff.type.ordinal()] = ff;
        }
    }

    public FontPack(final IFontProvider provider, final String name, final FontFamily... types) {
        this.id = provider.getNewPackId();
        this.provider = provider;
        this.name = name;
        this.types = new FontFamily[FontFamilyType.values().length];

        for (final FontFamily ff : types) {
            this.types[ff.type.ordinal()] = ff;
        }
    }

    public FontPack(final IFontProvider provider, final JSONObject object) throws JSONException {
        this.id = provider.getNewPackId();
        this.provider = provider;
        this.name = object.getString("name");
        this.types = new FontFamily[FontFamilyType.values().length];

        for (final FontFamilyType type : FontFamilyType.values()) {
            final JSONObject ffObject = object.optJSONObject(type.getResValue());
            if (ffObject != null) {
                final FontFamily ff = new FontFamily(type, ffObject);
                this.types[type.ordinal()] = ff;
            }
        }
    }

    public JSONObject toJSON() throws JSONException {
        final JSONObject object = new JSONObject();

        object.put("name", name);

        for (final FontFamily ff : types) {
            if (ff != null) {
                object.put(ff.type.getResValue(), ff.toJSON());
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

    public TypefaceEx getTypeface(final FontFamilyType type, final FontStyle style) {
        return provider.getTypeface(this, type, style);
    }

    @Override
    public String toString() {
        return name;
    }
}
