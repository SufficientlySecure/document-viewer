package org.emdev.common.settings.base;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonObjectPreferenceDefinition extends BasePreferenceDefinition<JSONObject> {

    public JsonObjectPreferenceDefinition(final int keyRes) {
        super(keyRes);
    }

    public JsonObjectPreferenceDefinition(final String key) {
        super(key);
    }

    @Override
    public JSONObject getPreferenceValue(final SharedPreferences prefs) {
        return getPreferenceValue(prefs, "{}");
    }

    public JSONObject getPreferenceValue(final SharedPreferences prefs, final String defValue) {
        try {
            final String value = prefs.getString(key, defValue);
            return new JSONObject(value);
        } catch (final JSONException ex) {
            LCTX.e("Settings processing error: [" + key + "] " + ex.getMessage());
        }
        return new JSONObject();
    }

    public void setPreferenceValue(final Editor edit, final JSONObject value) {
        edit.putString(key, value != null ? value.toString() : "{}");
    }

    @Override
    public void backup(final JSONObject root, final SharedPreferences prefs) throws JSONException {
        final JSONObject obj = getPreferenceValue(prefs);
        root.put(key, obj);
    }

    @Override
    public void restore(final JSONObject root, final Editor edit) throws JSONException {
        setPreferenceValue(edit, root.getJSONObject(key));
    }

}
