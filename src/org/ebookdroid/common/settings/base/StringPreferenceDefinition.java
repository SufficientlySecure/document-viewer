package org.ebookdroid.common.settings.base;

import org.ebookdroid.EBookDroidApp;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class StringPreferenceDefinition extends BasePreferenceDefinition {

    private final String defValue;

    public StringPreferenceDefinition(final int keyRes, final int defValRef) {
        super(keyRes);
        defValue = defValRef != 0 ? EBookDroidApp.context.getString(defValRef) : "";
    }

    public String getPreferenceValue(final SharedPreferences prefs) {
        return getPreferenceValue(prefs, defValue);
    }

    public String getPreferenceValue(final SharedPreferences prefs, final String defValue) {
        if (!prefs.contains(key)) {
            prefs.edit().putString(key, defValue).commit();
        }
        return prefs.getString(key, defValue);
    }

    public void setPreferenceValue(final Editor edit, final String value) {
        edit.putString(key, value);
    }

}
