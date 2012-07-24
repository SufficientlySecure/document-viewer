package org.emdev.common.fonts.data;

import java.util.Iterator;

import org.emdev.utils.collections.ArrayIterator;
import org.emdev.utils.enums.EnumUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FontFamily implements Iterable<FontInfo> {

    public final FontFamilyType type;

    protected final FontInfo[] fonts;

    public FontFamily(final FontFamilyType type, final FontInfo... fonts) {
        this.type = type;
        this.fonts = new FontInfo[FontStyle.values().length];
        for (final FontInfo fi : fonts) {
            this.fonts[fi.style.ordinal()] = fi;
        }
    }

    public FontFamily(final JSONObject object) throws JSONException {
        this.type = EnumUtils.getByResValue(FontFamilyType.class, object.getString("type"), FontFamilyType.SANS);
        this.fonts = new FontInfo[FontStyle.values().length];
        final JSONArray arr = object.getJSONArray("fonts");
        for (int i = 0, n = arr.length(); i < n; i++) {
            final FontInfo fi = new FontInfo(arr.getJSONObject(i));
            this.fonts[fi.style.ordinal()] = fi;
        }
    }

    public JSONObject toJSON() throws JSONException {
        final JSONObject object = new JSONObject();
        final JSONArray arr = new JSONArray();

        object.put("type", type.getResValue());
        object.put("fonts", arr);

        for (final FontInfo fi : fonts) {
            if (fi != null) {
                arr.put(fi.toJSON());
            }
        }

        return object;
    }

    @Override
    public Iterator<FontInfo> iterator() {
        return new ArrayIterator<FontInfo>(fonts);
    }

    public FontInfo getFont(final FontStyle style) {
        return fonts[style.ordinal()];
    }

    public String toString() {
        return type.getResValue();
    }
}
