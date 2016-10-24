package org.emdev.common.settings.base;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.emdev.BaseDroidApp;
import org.json.JSONObject;

public class BooleanPreferenceDefinition extends BasePreferenceDefinition<Boolean> {

    private final Boolean defValue;

    public BooleanPreferenceDefinition(final int keyRes, final int defValRef) {
        super(keyRes);
        defValue = Boolean.parseBoolean(BaseDroidApp.context.getString(defValRef));
    }

    @Override
    public Boolean getPreferenceValue(final SharedPreferences prefs) {
        return getPreferenceValue(prefs, defValue);
    }

    public boolean getPreferenceValue(final SharedPreferences prefs, final boolean defValue) {
        if (!prefs.contains(key)) {
            prefs.edit().putBoolean(key, defValue).commit();
        }
        return prefs.getBoolean(key, defValue);
    }

    public void setPreferenceValue(final Editor edit, final boolean value) {
        edit.putBoolean(key, value);
    }

    @Override
    public void restore(final JSONObject root, final Editor edit) {
        final String value = root.optString(key);
        setPreferenceValue(edit, value != null ? Boolean.parseBoolean(value) : defValue);
    }

    public boolean getDefaultValue() { return defValue; }
}
