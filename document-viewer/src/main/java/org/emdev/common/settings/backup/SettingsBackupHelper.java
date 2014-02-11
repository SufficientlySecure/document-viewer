package org.emdev.common.settings.backup;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.emdev.common.backup.BackupManager;
import org.emdev.common.settings.base.BasePreferenceDefinition;
import org.json.JSONException;
import org.json.JSONObject;

public class SettingsBackupHelper {

    private static final Map<Class<?>, List<BasePreferenceDefinition<?>>> defs = new HashMap<Class<?>, List<BasePreferenceDefinition<?>>>();

    public static List<BasePreferenceDefinition<?>> getPreferences(final Class<?> preferences) {
        List<BasePreferenceDefinition<?>> list = defs.get(preferences);
        if (list == null) {
            list = new ArrayList<BasePreferenceDefinition<?>>();

            final Field[] fields = preferences.getFields();
            for (final Field field : fields) {
                final int m = field.getModifiers();
                if (Modifier.isPublic(m) && Modifier.isStatic(m) && Modifier.isFinal(m)) {
                    if (BasePreferenceDefinition.class.isAssignableFrom(field.getType())) {
                        try {
                            final BasePreferenceDefinition<?> value = (BasePreferenceDefinition<?>) field.get(null);
                            list.add(value);
                        } catch (final Throwable th) {
                        }
                    }
                }
            }

            defs.put(preferences, list);
        }
        return list;
    }

    public static JSONObject backup(final String key, final SharedPreferences prefs, final Class<?> prefDefs) {
        final JSONObject backup = new JSONObject();

        final List<BasePreferenceDefinition<?>> list = SettingsBackupHelper.getPreferences(prefDefs);
        for (final BasePreferenceDefinition<?> p : list) {
            try {
                p.backup(backup, prefs);
            } catch (final JSONException ex) {
                BackupManager.LCTX.e("Error on [" + key + "] settings backup: " + ex.getMessage());
            }
        }
        return backup;
    }

    public static void restore(final String key, final SharedPreferences prefs, final Class<?> prefDefs,
            final JSONObject backup) {
        final Editor edit = prefs.edit();
        for (final BasePreferenceDefinition<?> p : getPreferences(prefDefs)) {
            try {
                p.restore(backup, edit);
            } catch (final JSONException ex) {
                BackupManager.LCTX.e("Error on [" + key + "] settings restore: " + ex.getMessage());
            }
        }
        edit.commit();
    }

}
