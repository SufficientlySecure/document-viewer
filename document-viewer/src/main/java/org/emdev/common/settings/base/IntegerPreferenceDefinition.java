package org.emdev.common.settings.base;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.emdev.BaseDroidApp;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.MathUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class IntegerPreferenceDefinition extends BasePreferenceDefinition<Integer> {

    public final int defValue;

    public final int minValue;

    public final int maxValue;

    public IntegerPreferenceDefinition(final int keyRes, final int defValRef) {
        super(keyRes);
        defValue = (int) (Long.decode(BaseDroidApp.context.getString(defValRef)) & 0xFFFFFFFF);
        minValue = Integer.MIN_VALUE;
        maxValue = Integer.MAX_VALUE;
    }

    public IntegerPreferenceDefinition(final int keyRes, final int defValRef, final int minValRef, final int maxValRef) {
        super(keyRes);
        defValue = (int) (Long.decode(BaseDroidApp.context.getString(defValRef)) & 0xFFFFFFFF);
        minValue = (int) (Long.decode(BaseDroidApp.context.getString(minValRef)) & 0xFFFFFFFF);
        maxValue = (int) (Long.decode(BaseDroidApp.context.getString(maxValRef)) & 0xFFFFFFFF);
    }

    @Override
    public Integer getPreferenceValue(final SharedPreferences prefs) {
        return getPreferenceValue(prefs, defValue);
    }

    public int getPreferenceValue(final SharedPreferences prefs, final int defValue) {
        if (!prefs.contains(key)) {
            prefs.edit().putString(key, Integer.toString(defValue)).commit();
        }
        int value = defValue;
        try {
            final String str = prefs.getString(key, "");
            if (LengthUtils.isNotEmpty(str)) {
                value = Integer.parseInt(str);
            }
        } catch (final Exception e) {
            LCTX.e("Settings processing error: [" + key + "] " + e.getMessage());
        }
        return MathUtils.adjust(value, minValue, maxValue);
    }

    public void setPreferenceValue(final Editor edit, final int value) {
        edit.putString(key, Integer.toString(value));
    }

    @Override
    public void restore(final JSONObject root, final Editor edit) throws JSONException {
        final String value = root.optString(key);
        try {
            setPreferenceValue(edit, LengthUtils.isNotEmpty(value) ? Integer.valueOf(value) : defValue);
        } catch (NumberFormatException ex) {
            LCTX.e("Settings restoring error: [" + key + "] " + ex.getMessage());
        }
    }
}
