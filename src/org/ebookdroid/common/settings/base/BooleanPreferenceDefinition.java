package org.ebookdroid.common.settings.base;

import org.ebookdroid.EBookDroidApp;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class BooleanPreferenceDefinition extends BasePreferenceDefinition {

    private final Boolean defValue;

    public BooleanPreferenceDefinition(final int keyRes, final int defValRef) {
        super(keyRes);
        defValue = Boolean.parseBoolean(EBookDroidApp.context.getString(defValRef));
    }

    public boolean getPreferenceValue(final SharedPreferences prefs) {
        return getPreferenceValue(prefs, defValue);
    }

    public boolean getPreferenceValue(final SharedPreferences prefs, boolean defValue) {
        if (!prefs.contains(key)) {
            prefs.edit().putBoolean(key, defValue).commit();
        }
        return prefs.getBoolean(key, defValue);
    }

    public void setPreferenceValue(final Editor edit, final boolean value) {
        edit.putBoolean(key, value);
    }

}
