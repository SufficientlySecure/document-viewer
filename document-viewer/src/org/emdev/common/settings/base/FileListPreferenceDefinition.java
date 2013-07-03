package org.emdev.common.settings.base;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.emdev.BaseDroidApp;
import org.emdev.utils.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FileListPreferenceDefinition extends BasePreferenceDefinition<Set<String>> {

    private final String defValue;

    public FileListPreferenceDefinition(final int keyRes, final int defValRes) {
        super(keyRes);
        defValue = BaseDroidApp.context.getString(defValRes);
    }

    @Override
    public Set<String> getPreferenceValue(final SharedPreferences prefs) {
        if (!prefs.contains(key)) {
            prefs.edit().putString(key, defValue).commit();
        }
        return StringUtils.split(File.pathSeparator, prefs.getString(key, defValue));
    }

    public void setPreferenceValue(final Editor edit, final Set<String> paths) {
        edit.putString(key, StringUtils.merge(File.pathSeparator, paths));
    }

    @Override
    public void backup(final JSONObject root, final SharedPreferences prefs) throws JSONException {
        final Set<String> values = getPreferenceValue(prefs);
        final JSONArray arr = new JSONArray();
        for (final String v : values) {
            arr.put(v);
        }
        root.put(key, arr);
    }

    @Override
    public void restore(final JSONObject root, final Editor edit) throws JSONException {
        final Set<String> values = new LinkedHashSet<String>();
        final JSONArray arr = root.optJSONArray(key);
        if (arr != null) {
            for (int i = 0, n = arr.length(); i < n; i++) {
                values.add(arr.getString(i));
            }
        }
        setPreferenceValue(edit, values);
    }

}
