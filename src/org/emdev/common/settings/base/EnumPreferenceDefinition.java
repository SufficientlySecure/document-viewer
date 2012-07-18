package org.emdev.common.settings.base;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.emdev.BaseDroidApp;
import org.emdev.utils.enums.EnumUtils;
import org.emdev.utils.enums.ResourceConstant;
import org.json.JSONObject;

public class EnumPreferenceDefinition<E extends Enum<E> & ResourceConstant> extends BasePreferenceDefinition<E> {

    private final Class<E> enumClass;
    private final E defValue;

    public EnumPreferenceDefinition(final Class<E> enumClass, final int keyRes, final int defValRef) {
        super(keyRes);
        this.enumClass = enumClass;
        this.defValue = EnumUtils.getByResValue(enumClass, BaseDroidApp.context.getString(defValRef), null);
    }

    @Override
    public E getPreferenceValue(final SharedPreferences prefs) {
        return getPreferenceValue(prefs, defValue);
    }

    public E getPreferenceValue(final SharedPreferences prefs, final E defValue) {
        if (!prefs.contains(key)) {
            prefs.edit().putString(key, defValue.getResValue()).commit();
        }
        return EnumUtils.getByResValue(enumClass, prefs.getString(key, null), defValue);
    }

    public void setPreferenceValue(final Editor edit, final E value) {
        if (value != null) {
            edit.putString(key, value.getResValue());
        } else {
            edit.remove(key);
        }
    }

    @Override
    public void restore(final JSONObject root, final Editor edit) {
        final String value = root.optString(key);
        setPreferenceValue(edit, EnumUtils.getByName(enumClass, value, defValue));
    }
}
