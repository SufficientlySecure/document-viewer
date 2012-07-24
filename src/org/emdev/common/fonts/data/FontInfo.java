package org.emdev.common.fonts.data;

import org.emdev.utils.enums.EnumUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class FontInfo {

    public final String path;

    public final FontStyle style;

    public FontInfo(final String path, final FontStyle style) {
        this.path = path;
        this.style = style;
    }

    public FontInfo(final JSONObject object) throws JSONException {
        this.path = object.getString("path");
        this.style = EnumUtils.getByResValue(FontStyle.class, object.optString("style"), FontStyle.REGULAR);
    }

    public JSONObject toJSON() throws JSONException {
        final JSONObject object = new JSONObject();
        object.put("path", path);
        object.put("style", style.getResValue());
        return object;
    }

    public String toString() {
        try {
            return toJSON().toString();
        } catch (JSONException ex) {
        }
        return path;
    }
}
