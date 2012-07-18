package org.emdev.ui.preference;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;

import org.emdev.common.settings.base.JsonObjectPreferenceDefinition;
import org.emdev.utils.WidgetUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonCheckBoxPreference extends CheckBoxPreference {

    private JsonObjectPreferenceDefinition def;

    private String jsonProperty;

    private Object defValue;

    public JsonCheckBoxPreference(final Context context) {
        super(context);
    }

    public JsonCheckBoxPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        def = new JsonObjectPreferenceDefinition(getKey());
        jsonProperty = WidgetUtils.getStringAttribute(context, attrs, WidgetUtils.EBOOKDROID_NS, WidgetUtils.ATTR_JSON_PROPERTY, null);
        defValue = WidgetUtils.getBooleanAttribute(context, attrs, WidgetUtils.ANDROID_NS, WidgetUtils.ATTR_DEFAULT_VALUE, null);
        setKey(def.key + "." + jsonProperty);
    }

    public JsonCheckBoxPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        def = new JsonObjectPreferenceDefinition(getKey());
        jsonProperty = WidgetUtils.getStringAttribute(context, attrs, WidgetUtils.EBOOKDROID_NS, WidgetUtils.ATTR_JSON_PROPERTY, null);
        defValue = WidgetUtils.getBooleanAttribute(context, attrs, WidgetUtils.ANDROID_NS, WidgetUtils.ATTR_DEFAULT_VALUE, null);
        setKey(def.key + "." + jsonProperty);
    }

    @Override
    protected boolean shouldPersist() {
        return def != null && jsonProperty != null && super.shouldPersist();
    }

    @Override
    protected void onSetInitialValue(final boolean restoreValue, final Object defaultValue) {
        // By now, we know if we are persistent.
        if (!shouldPersist() || !containsProperty()) {
            if (defValue != null) {
                super.onSetInitialValue(false, defValue);
            }
        } else {
            super.onSetInitialValue(true, null);
        }
    }

    private boolean containsProperty() {
        return def.getPreferenceValue(getSharedPreferences()).has(jsonProperty);
    }

    @Override
    protected boolean getPersistedBoolean(final boolean defaultReturnValue) {
        if (def != null && jsonProperty != null) {
            final SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            final JSONObject object = def.getPreferenceValue(prefs);
            try {
                return object.has(jsonProperty) ? object.getBoolean(jsonProperty) : defaultReturnValue;
            } catch (final JSONException ex) {
            }
        }
        return defaultReturnValue;
    }

    @Override
    protected boolean persistBoolean(final boolean value) {
        if (shouldPersist()) {
            final SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            final JSONObject object = def.getPreferenceValue(prefs);
            try {
                final boolean oldValue = object.has(jsonProperty) ? object.getBoolean(jsonProperty) : !value;
                if (value == oldValue) {
                    // It's already there, so the same as persisting
                    return true;
                }

                object.put(jsonProperty, value);
                final SharedPreferences.Editor editor = prefs.edit();
                def.setPreferenceValue(editor, object);
                editor.commit();
            } catch (final JSONException ex) {
            }

            return true;
        }
        return false;
    }
}
