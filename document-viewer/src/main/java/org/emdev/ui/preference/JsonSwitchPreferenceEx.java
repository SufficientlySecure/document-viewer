package org.emdev.ui.preference;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.CompoundButton;

import org.emdev.common.settings.base.JsonObjectPreferenceDefinition;
import org.emdev.utils.WidgetUtils;
import org.json.JSONException;
import org.json.JSONObject;

@TargetApi(15)
public class JsonSwitchPreferenceEx extends SwitchPreference {

    private JsonObjectPreferenceDefinition def;

    private String jsonProperty;

    private Object defValue;

    private final Listener mListener = new Listener();

    private class Listener implements CompoundButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
            if (!callChangeListener(isChecked)) {
                // Listener didn't like it, change it back.
                // CompoundButton will make sure we don't recurse.
                buttonView.setChecked(!isChecked);
                return;
            }

            JsonSwitchPreferenceEx.this.setChecked(isChecked);
        }
    }

    public JsonSwitchPreferenceEx(final Context context) {
        super(context);
    }

    public JsonSwitchPreferenceEx(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        def = new JsonObjectPreferenceDefinition(getKey());
        jsonProperty = WidgetUtils.getStringAttribute(context, attrs, WidgetUtils.EBOOKDROID_NS, WidgetUtils.ATTR_JSON_PROPERTY, null);
        defValue = WidgetUtils.getBooleanAttribute(context, attrs, WidgetUtils.ANDROID_NS, WidgetUtils.ATTR_DEFAULT_VALUE, null);
        setKey(def.key + "." + jsonProperty);
    }

    public JsonSwitchPreferenceEx(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        def = new JsonObjectPreferenceDefinition(getKey());
        jsonProperty = WidgetUtils.getStringAttribute(context, attrs, WidgetUtils.EBOOKDROID_NS, WidgetUtils.ATTR_JSON_PROPERTY, null);
        defValue = WidgetUtils.getBooleanAttribute(context, attrs, WidgetUtils.ANDROID_NS, WidgetUtils.ATTR_DEFAULT_VALUE, null);
        setKey(def.key + "." + jsonProperty);
    }

    @Override
    protected void onBindView(final View view) {

        final Checkable cview = getCheckableView(view);
        if (cview instanceof CompoundButton) {
            final CompoundButton btn = (CompoundButton) cview;
            btn.setOnCheckedChangeListener(mListener);
        }
        super.onBindView(view);
    }

    protected Checkable getCheckableView(final View view) {
        if (view instanceof Checkable) {
            return (Checkable) view;
        } else if (view instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                final View v = group.getChildAt(i);
                if (v instanceof Checkable) {
                    return (Checkable) v;
                }
            }
        }
        return null;
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
