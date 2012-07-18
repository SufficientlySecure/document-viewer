package org.emdev.common.settings.base;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.emdev.BaseDroidApp;
import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class BasePreferenceDefinition<T> {

    public static final LogContext LCTX = LogManager.root().lctx("Settigns");

    public final String key;

    public BasePreferenceDefinition(final int keyRes) {
        key = BaseDroidApp.context.getString(keyRes);
    }

    public BasePreferenceDefinition(final String key) {
        this.key = key;
    }

    public abstract T getPreferenceValue(final SharedPreferences prefs);

    public void backup(final JSONObject root, final SharedPreferences prefs) throws JSONException {
        final T value = getPreferenceValue(prefs);
        if (value != null) {
            root.put(key, value.toString());
        }
    }

    public abstract void restore(JSONObject root, Editor edit) throws JSONException;

    @Override
    @SuppressWarnings("rawtypes")
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BasePreferenceDefinition) {
            final BasePreferenceDefinition that = (BasePreferenceDefinition) obj;
            return this.key.equals(that.key);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public String toString() {
        return this.key;
    }
}
