package org.ebookdroid.core.touch;

import org.ebookdroid.R;
import org.ebookdroid.core.actions.ActionEx;
import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.settings.AppSettings;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.utils.LengthUtils;

import android.graphics.Rect;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TouchManager {

    private static final LogContext LCTX = LogContext.ROOT.lctx("Actions");

    public static final String DEFAULT_PROFILE = "DocumentView.Default";

    private static final Map<String, TouchProfile> profiles = new HashMap<String, TouchManager.TouchProfile>();

    private static final LinkedList<TouchProfile> stack = new LinkedList<TouchProfile>();

    public static void applyOldStyleSettings(final AppSettings newSettings) {
        profiles.clear();
        stack.clear();

        boolean fromJSON = false;
        String str = newSettings.getTouchProfiles();
        if (LengthUtils.isNotEmpty(str)) {
            try {
                List<TouchProfile> list = fromJSON(str);
                for (TouchProfile p : list) {
                    profiles.put(p.name, p);
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
            fromJSON = profiles.containsKey(DEFAULT_PROFILE) && profiles.containsKey(TouchManagerView.TMV_PROFILE);
        }

        if (!fromJSON) {
            final TouchProfile tp = addProfile(TouchManagerView.TMV_PROFILE);
            {
                final Region r = tp.addRegion(0, 0, 100, 100);
                r.setAction(Touch.DoubleTap, true, R.id.actions_toggleTouchManagerView);
            }

            TouchProfile def = addProfile(DEFAULT_PROFILE);
            {
                final Region r = def.addRegion(0, 0, 100, 100);
                r.setAction(Touch.DoubleTap, newSettings.getZoomByDoubleTap(), R.id.mainmenu_zoom);
                r.setAction(Touch.LongTap, true, R.id.actions_openOptionsMenu);
            }
            {
                final Region r = def.addRegion(0, 0, 100, newSettings.getTapSize());
                r.setAction(Touch.SingleTap, newSettings.getTapScroll(), R.id.actions_verticalConfigScrollUp);
            }
            {
                final Region r = def.addRegion(0, 100 - newSettings.getTapSize(), 100, 100);
                r.setAction(Touch.SingleTap, newSettings.getTapScroll(), R.id.actions_verticalConfigScrollDown);
            }

            try {
                JSONObject json = toJSON();
                SettingsManager.getAppSettings().updateTouchProfiles(json.toString());
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }

        stack.addFirst(profiles.get(DEFAULT_PROFILE));
    }

    public static Integer getAction(final Touch type, final float x, final float y, final float width,
            final float height) {
        return stack.peek().getAction(type, x, y, width, height);
    }

    public static TouchProfile addProfile(String name) {
        TouchProfile tp = new TouchProfile(name);
        profiles.put(tp.name, tp);
        return tp;
    }

    public static TouchProfile pushProfile(String name) {
        TouchProfile prev = stack.isEmpty() ? null : stack.peek();
        TouchProfile tp = profiles.get(name);
        if (tp != null) {
            stack.addFirst(tp);
        }
        return prev;
    }

    public static TouchProfile popProfile() {
        if (stack.size() > 1) {
            stack.removeFirst();
        }
        return stack.peek();
    }

    public static JSONObject toJSON() throws JSONException {
        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();
        for (TouchProfile p : profiles.values()) {
            array.put(p.toJSON());
        }
        object.put("profiles", array);
        return object;
    }

    private static List<TouchProfile> fromJSON(String str) throws JSONException {
        List<TouchProfile> list = new ArrayList<TouchProfile>();

        JSONObject root = new JSONObject(str);

        JSONArray profiles = root.getJSONArray("profiles");
        for (int pIndex = 0; pIndex < profiles.length(); pIndex++) {
            JSONObject p = (JSONObject) profiles.getJSONObject(pIndex);
            TouchProfile profile = TouchProfile.fromJSON(p);
            list.add(profile);
        }
        return list;
    }

    public static class TouchProfile {

        public final String name;
        final LinkedList<Region> regions = new LinkedList<Region>();

        public TouchProfile(String name) {
            super();
            this.name = name;
        }

        public ListIterator<Region> regions() {
            return regions.listIterator();
        }

        public void clear() {
            regions.clear();
        }

        public Integer getAction(final Touch type, final float x, final float y, final float width, final float height) {
            LCTX.d("getAction(" + type + ", " + x + ", " + y + ", " + width + ", " + height + ")");
            for (final Region r : regions) {
                final RectF rect = r.getActualRect(width, height);
                LCTX.d("Region: " + rect);
                if (rect.left <= x && x < rect.right && rect.top <= y && y < rect.bottom) {
                    final ActionRef action = r.getAction(type);
                    LCTX.d("Action: " + action);
                    if (action != null && action.enabled) {
                        return action.id;
                    }
                }
            }
            return null;
        }

        public Region addRegion(final int left, final int top, final int right, final int bottom) {
            final Region r = new Region(new Rect(left, top, right, bottom));
            return addRegion(r);
        }

        public Region addRegion(final Region r) {
            regions.addFirst(r);
            return r;
        }

        public void removeRegion(final Region r) {
            regions.remove(r);
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder(this.getClass().getSimpleName());
            buf.append("[");
            buf.append("name").append("=").append(name);
            buf.append(", ");
            buf.append("regions").append("=").append(regions);
            buf.append("]");

            return buf.toString();
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject object = new JSONObject();
            object.put("name", this.name);

            JSONArray array = new JSONArray();
            for (Region r : regions) {
                array.put(r.toJSON());
            }
            object.put("regions", array);

            return object;
        }

        public static TouchProfile fromJSON(JSONObject json) throws JSONException {
            TouchProfile profile = new TouchProfile(json.getString("name"));

            JSONArray regions = json.getJSONArray("regions");
            for (int rIndex = 0; rIndex < regions.length(); rIndex++) {
                JSONObject r = regions.getJSONObject(rIndex);
                Region region = Region.fromJSON(r);
                profile.addRegion(region);
            }
            return profile;
        }
    }

    public static enum Touch {
        SingleTap, DoubleTap, LongTap;
    }

    public static class Region {

        private final Rect rect;
        private final ActionRef[] actions = new ActionRef[Touch.values().length];

        public Region(final Rect r) {
            rect = r;
        }

        public ActionRef getAction(final Touch type) {
            return actions[type.ordinal()];
        }

        public ActionRef setAction(final Touch type, final boolean enabled, final int id) {
            final ActionRef a = new ActionRef(type, enabled, id);
            actions[type.ordinal()] = a;
            return a;
        }

        public RectF getActualRect(final float width, final float height) {
            return new RectF(width * rect.left / 100.0f, height * rect.top / 100.0f, width * rect.right / 100.0f,
                    height * rect.bottom / 100.0f);
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder(this.getClass().getSimpleName());
            buf.append("[");
            buf.append("rect").append("=").append(rect);
            buf.append(", ");
            buf.append("actions").append("=").append(Arrays.toString(actions));
            buf.append("]");

            return buf.toString();
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject object = new JSONObject();

            JSONObject r = new JSONObject();
            r.put("left", rect.left);
            r.put("top", rect.top);
            r.put("right", rect.right);
            r.put("bottom", rect.bottom);
            object.put("rect", r);

            JSONArray a = new JSONArray();
            for (ActionRef action : actions) {
                if (action != null) {
                    a.put(action.toJSON());
                }
            }
            object.put("actions", a);
            return object;
        }

        public static Region fromJSON(JSONObject json) throws JSONException {
            JSONObject r = (JSONObject) json.getJSONObject("rect");
            Rect rect = new Rect(r.getInt("left"), r.getInt("top"), r.getInt("right"), r.getInt("bottom"));
            
            Region region = new Region(rect);
            JSONArray actions = json.getJSONArray("actions");
            for (int aIndex = 0; aIndex < actions.length(); aIndex++) {
                JSONObject a = actions.getJSONObject(aIndex);
                region.setAction(Touch.valueOf(a.getString("type")), a.getBoolean("enabled"), a.getInt("id"));
            }
            return region;
        }
    }

    public static class ActionRef {

        public final Touch type;
        public final int id;
        public boolean enabled;

        public ActionRef(final Touch type, final boolean enabled, final int id) {
            this.type = type;
            this.enabled = enabled;
            this.id = id;
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject object = new JSONObject();
            object.put("type", type.name());
            object.put("id", id);
            object.put("enabled", enabled);
            return object;
        }

        @Override
        public String toString() {
            return type + ", " + ActionEx.getActionName(id) + ",  " + enabled;
        }
    }
}
