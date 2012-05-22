package org.ebookdroid.common.settings.base;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonObjectPreferenceDefinition extends BasePreferenceDefinition {

    public JsonObjectPreferenceDefinition(int keyRes, final int defValRef) {
        super(keyRes);
    }

    public JSONObject getPreferenceValue(final SharedPreferences prefs) {
        return getPreferenceValue(prefs, "{}");
    }

    public JSONObject getPreferenceValue(final SharedPreferences prefs, final String defValue) {
        try {
            return new JSONObject(prefs.getString(key, defValue));
        } catch (JSONException ex) {
            LCTX.e("Settings processing error: [" + key + "] " + ex.getMessage());
        }
        return new JSONObject();
    }

    public void setPreferenceValue(final Editor edit, final JSONObject value) {
        edit.putString(key, value != null ? value.toString() : "{}");
    }
}
