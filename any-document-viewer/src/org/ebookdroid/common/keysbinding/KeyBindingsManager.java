package org.ebookdroid.common.keysbinding;

import org.ebookdroid.R;
import org.ebookdroid.common.settings.AppSettings;

import android.view.KeyEvent;

import java.lang.reflect.Field;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.ui.actions.ActionEx;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.collections.SparseArrayEx;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class KeyBindingsManager {

    private static final LogContext LCTX = LogManager.root().lctx("Actions");

    private static SparseArrayEx<ActionRef> actions = new SparseArrayEx<KeyBindingsManager.ActionRef>();

    private static SparseArrayEx<String> keyLabels;

    public static void loadFromSettings(final AppSettings newSettings) {
        actions.clear();

        boolean fromJSON = false;
        final String str = newSettings.keysBinding;
        if (LengthUtils.isNotEmpty(str)) {
            try {
                fromJSON(str);
                fromJSON = true;
            } catch (final Throwable ex) {
                LCTX.e("Error on tap configuration load: ", ex);
            }
        }

        if (!fromJSON) {
            addAction(R.id.actions_verticalConfigScrollUp, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_VOLUME_UP);
            addAction(R.id.actions_verticalConfigScrollDown, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN);

            persist();
        }
    }

    public static void persist() {
        try {
            final JSONObject json = toJSON();
            AppSettings.updateKeysBinding(json.toString());
        } catch (final JSONException ex) {
            LCTX.e("Unexpected error: ", ex);
        }
    }

    public static JSONObject toJSON() throws JSONException {
        final JSONObject object = new JSONObject();
        final JSONArray array = new JSONArray();
        for (final ActionRef ref : actions) {
            array.put(ref.toJSON());
        }
        object.put("actions", array);
        return object;
    }

    public static Integer getAction(final KeyEvent ev) {
        return getAction(ev.getKeyCode());
    }

    public static Integer getAction(final int code) {
        final ActionRef ref = actions.get(code);
        return ref != null && ref.enabled ? Integer.valueOf(ref.id) : null;
    }

    public static void addAction(final int id, final int... keys) {
        for (final int key : keys) {
            actions.append(key, new ActionRef(key, id, true));
        }
    }

    public static void removeAction(final int code) {
        actions.remove(code);
    }

    public static String keyCodeToString(final int code) {
        if (keyLabels == null) {
            keyLabels = new SparseArrayEx<String>();
            for (final Field f : KeyEvent.class.getFields()) {
                if (f.getName().startsWith("KEYCODE_")) {
                    try {
                        final Integer value = f.getInt(null);
                        final String label = f.getName().substring("KEYCODE_".length());
                        keyLabels.append(value, label.replaceAll("_", " "));
                    } catch (final Throwable th) {
                        th.printStackTrace();
                    }
                }
            }
        }
        final String label = keyLabels.get(code);
        return label != null ? label : Integer.toString(code);
    }

    private static void fromJSON(final String str) throws JSONException {
        final JSONObject root = new JSONObject(str);

        final JSONArray list = root.getJSONArray("actions");
        for (int pIndex = 0; pIndex < list.length(); pIndex++) {
            final JSONObject p = list.getJSONObject(pIndex);
            final ActionRef ref = ActionRef.fromJSON(p);
            actions.append(ref.code, ref);
        }
    }

    public static class ActionRef {

        public final int code;
        public final int id;
        public final String name;
        public boolean enabled;

        public ActionRef(final int code, final int id, final boolean enabled) {
            this.code = code;
            this.id = id;
            this.name = ActionEx.getActionName(id);
            this.enabled = enabled;
        }

        public static ActionRef fromJSON(final JSONObject json) throws JSONException {
            final int code = json.getInt("code");
            final String name = json.getString("name");
            final Integer id = ActionEx.getActionId(name);
            return new ActionRef(code, id, true);
        }

        public JSONObject toJSON() throws JSONException {
            final JSONObject object = new JSONObject();
            object.put("code", code);
            object.put("name", name);
            object.put("enabled", enabled);
            return object;
        }

        @Override
        public String toString() {
            return "(" + code + ", " + name + ", " + enabled + ")";
        }
    }

    public static class KeyInfo {

        public final int code;
        public final String label;

        public KeyInfo(final int code, final String label) {
            this.code = code;
            this.label = label;
        }

    }
}
