package org.ebookdroid.common.settings.base;

import org.ebookdroid.EBookDroidApp;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.emdev.utils.LengthUtils;
import org.emdev.utils.MathUtils;

public class IntegerPreferenceDefinition extends BasePreferenceDefinition {

    public final int defValue;

    public final int minValue;

    public final int maxValue;

    public IntegerPreferenceDefinition(final int keyRes, final int defValRef) {
        super(keyRes);
        defValue = Integer.parseInt(EBookDroidApp.context.getString(defValRef));
        minValue = Integer.MIN_VALUE;
        maxValue = Integer.MAX_VALUE;
    }

    public IntegerPreferenceDefinition(final int keyRes, final int defValRef, final int minValRef, final int maxValRef) {
        super(keyRes);
        defValue = Integer.parseInt(EBookDroidApp.context.getString(defValRef));
        minValue = Integer.parseInt(EBookDroidApp.context.getString(minValRef));
        maxValue = Integer.parseInt(EBookDroidApp.context.getString(maxValRef));
    }

    public int getPreferenceValue(final SharedPreferences prefs) {
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
}
