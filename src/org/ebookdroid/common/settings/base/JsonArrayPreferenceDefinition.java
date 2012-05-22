package org.ebookdroid.common.settings.base;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.json.JSONArray;
import org.json.JSONException;

public class JsonArrayPreferenceDefinition extends BasePreferenceDefinition {

    public JsonArrayPreferenceDefinition(final int keyRes) {
        super(keyRes);
    }

    public JSONArray getPreferenceValue(final SharedPreferences prefs) {
        return getPreferenceValue(prefs, "[]");
    }

    public JSONArray getPreferenceValue(final SharedPreferences prefs, final String defValue) {
        try {
            final String val = prefs.getString(key, defValue);
            return new JSONArray(val);
        } catch (final JSONException ex) {
            LCTX.e("Settings processing error: [" + key + "] " + ex.getMessage());
        }
        return new JSONArray();
    }

    public void setPreferenceValue(final Editor edit, final JSONArray value) {
        final String val = value != null ? value.toString() : "[]";
        edit.putString(key, val);
    }
}
