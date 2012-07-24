package org.emdev.common.fonts.data;

import java.util.Iterator;

import org.emdev.utils.LengthUtils;
import org.emdev.utils.collections.ArrayIterator;
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

    public FontFamily(final FontFamilyType type, final JSONObject object) throws JSONException {
        this.type = type;
        this.fonts = new FontInfo[FontStyle.values().length];

        for (final FontStyle style : FontStyle.values()) {
            final String path = object.optString(style.getResValue());
            if (LengthUtils.isNotEmpty(path)) {
                final FontInfo fi = new FontInfo(path, style);
                this.fonts[style.ordinal()] = fi;
            }
        }
    }

    public JSONObject toJSON() throws JSONException {
        final JSONObject object = new JSONObject();

        for (final FontInfo fi : fonts) {
            if (fi != null && LengthUtils.isNotEmpty(fi.path)) {
                object.put(fi.style.getResValue(), fi.path);
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

    @Override
    public String toString() {
        return type.getResValue();
    }
}
