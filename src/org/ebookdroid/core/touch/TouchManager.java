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
        final String str = newSettings.getTouchProfiles();
        if (LengthUtils.isNotEmpty(str)) {
            try {
                final List<TouchProfile> list = fromJSON(str);
                for (final TouchProfile p : list) {
                    profiles.put(p.name, p);
                }
            } catch (final Throwable ex) {
                ex.printStackTrace();
            }
            fromJSON = profiles.containsKey(DEFAULT_PROFILE) && profiles.containsKey(TouchManagerView.TMV_PROFILE);
        }

        if (!fromJSON) {
            final TouchProfile tp = addProfile(TouchManagerView.TMV_PROFILE);
            {
                final Region r = tp.addRegion(0, 0, 100, 100);
                r.setAction(Touch.DoubleTap, R.id.actions_toggleTouchManagerView, true);
            }

            final TouchProfile def = addProfile(DEFAULT_PROFILE);
            {
                final Region r = def.addRegion(0, 0, 100, 100);
                r.setAction(Touch.DoubleTap, R.id.mainmenu_zoom, newSettings.getZoomByDoubleTap());
                r.setAction(Touch.LongTap, R.id.actions_openOptionsMenu, true);
            }
            {
                final Region r = def.addRegion(0, 0, 100, newSettings.getTapSize());
                r.setAction(Touch.SingleTap, R.id.actions_verticalConfigScrollUp, newSettings.getTapScroll());
            }
            {
                final Region r = def.addRegion(0, 100 - newSettings.getTapSize(), 100, 100);
                r.setAction(Touch.SingleTap, R.id.actions_verticalConfigScrollDown, newSettings.getTapScroll());
            }

            try {
                final JSONObject json = toJSON();
                SettingsManager.getAppSettings().updateTouchProfiles(json.toString());
            } catch (final JSONException ex) {
                ex.printStackTrace();
            }
        } else {
            setActionEnabled(DEFAULT_PROFILE, R.id.mainmenu_zoom, newSettings.getZoomByDoubleTap());
            setActionEnabled(DEFAULT_PROFILE, R.id.actions_verticalConfigScrollUp, newSettings.getTapScroll(), 0, 0,
                    100, newSettings.getTapSize());
            setActionEnabled(DEFAULT_PROFILE, R.id.actions_verticalConfigScrollDown, newSettings.getTapScroll(), 0,
                    100 - newSettings.getTapSize(), 100, 100);
        }

        stack.addFirst(profiles.get(DEFAULT_PROFILE));
    }

    public static void setActionEnabled(final String profile, final int id, final boolean enabled) {
        final TouchProfile tp = profiles.get(profile);
        for (final Region r : tp.regions) {
            for (final ActionRef a : r.actions) {
                if (a != null && a.id == id) {
                    a.enabled = enabled;
                }
            }
        }
    }

    public static void setActionEnabled(final String profile, final int id, final boolean enabled, final int left,
            final int top, final int right, final int bottom) {
        final TouchProfile tp = profiles.get(profile);
        for (final Region r : tp.regions) {
            for (final ActionRef a : r.actions) {
                if (a != null && a.id == id) {
                    a.enabled = enabled;
                    r.rect.left = left;
                    r.rect.top = top;
                    r.rect.right = right;
                    r.rect.bottom = bottom;
                    return;
                }
            }
        }
    }

    public static Integer getAction(final Touch type, final float x, final float y, final float width,
            final float height) {
        return stack.peek().getAction(type, x, y, width, height);
    }

    public static TouchProfile addProfile(final String name) {
        final TouchProfile tp = new TouchProfile(name);
        profiles.put(tp.name, tp);
        return tp;
    }

    public static TouchProfile pushProfile(final String name) {
        final TouchProfile prev = stack.isEmpty() ? null : stack.peek();
        final TouchProfile tp = profiles.get(name);
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
        final JSONObject object = new JSONObject();
        final JSONArray array = new JSONArray();
        for (final TouchProfile p : profiles.values()) {
            array.put(p.toJSON());
        }
        object.put("profiles", array);
        return object;
    }

    private static List<TouchProfile> fromJSON(final String str) throws JSONException {
        final List<TouchProfile> list = new ArrayList<TouchProfile>();

        final JSONObject root = new JSONObject(str);

        final JSONArray profiles = root.getJSONArray("profiles");
        for (int pIndex = 0; pIndex < profiles.length(); pIndex++) {
            final JSONObject p = profiles.getJSONObject(pIndex);
            final TouchProfile profile = TouchProfile.fromJSON(p);
            list.add(profile);
        }
        return list;
    }

    public static class TouchProfile {

        public final String name;
        final LinkedList<Region> regions = new LinkedList<Region>();

        public TouchProfile(final String name) {
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
            final StringBuilder buf = new StringBuilder(this.getClass().getSimpleName());
            buf.append("[");
            buf.append("name").append("=").append(name);
            buf.append(", ");
            buf.append("regions").append("=").append(regions);
            buf.append("]");

            return buf.toString();
        }

        public JSONObject toJSON() throws JSONException {
            final JSONObject object = new JSONObject();
            object.put("name", this.name);

            final JSONArray array = new JSONArray();
            for (final Region r : regions) {
                array.put(r.toJSON());
            }
            object.put("regions", array);

            return object;
        }

        public static TouchProfile fromJSON(final JSONObject json) throws JSONException {
            final TouchProfile profile = new TouchProfile(json.getString("name"));

            final JSONArray regions = json.getJSONArray("regions");
            for (int rIndex = 0; rIndex < regions.length(); rIndex++) {
                final JSONObject r = regions.getJSONObject(rIndex);
                final Region region = Region.fromJSON(r);
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

        public ActionRef setAction(final Touch type, final int id, final boolean enabled) {
            final ActionRef a = new ActionRef(type, id, enabled);
            actions[type.ordinal()] = a;
            return a;
        }

        public RectF getActualRect(final float width, final float height) {
            return new RectF(width * rect.left / 100.0f, height * rect.top / 100.0f, width * rect.right / 100.0f,
                    height * rect.bottom / 100.0f);
        }

        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder(this.getClass().getSimpleName());
            buf.append("[");
            buf.append("rect").append("=").append(rect);
            buf.append(", ");
            buf.append("actions").append("=").append(Arrays.toString(actions));
            buf.append("]");

            return buf.toString();
        }

        public JSONObject toJSON() throws JSONException {
            final JSONObject object = new JSONObject();

            final JSONObject r = new JSONObject();
            r.put("left", rect.left);
            r.put("top", rect.top);
            r.put("right", rect.right);
            r.put("bottom", rect.bottom);
            object.put("rect", r);

            final JSONArray a = new JSONArray();
            for (final ActionRef action : actions) {
                if (action != null) {
                    a.put(action.toJSON());
                }
            }
            object.put("actions", a);
            return object;
        }

        public static Region fromJSON(final JSONObject json) throws JSONException {
            final JSONObject r = json.getJSONObject("rect");
            final Rect rect = new Rect(r.getInt("left"), r.getInt("top"), r.getInt("right"), r.getInt("bottom"));

            final Region region = new Region(rect);
            final JSONArray actions = json.getJSONArray("actions");
            for (int aIndex = 0; aIndex < actions.length(); aIndex++) {
                try {
                    final JSONObject a = actions.getJSONObject(aIndex);
                    final Touch type = Touch.valueOf(a.getString("type"));
                    final String name = a.getString("name");
                    final Integer id = ActionEx.getActionId(name);
                    if (id != null) {
                        region.setAction(type, id, a.getBoolean("enabled"));
                    } else {
                        LCTX.e("Unknown action name: " + name);
                    }
                } catch (final JSONException ex) {
                    throw new JSONException("Old perssitent format found. Touch action are returned to default ones: "
                            + ex.getMessage());
                }
            }
            return region;
        }
    }

    public static class ActionRef {

        public final Touch type;
        public final int id;
        public boolean enabled;

        public ActionRef(final Touch type, final int id, final boolean enabled) {
            this.type = type;
            this.id = id;
            this.enabled = enabled;
        }

        public JSONObject toJSON() throws JSONException {
            final JSONObject object = new JSONObject();
            object.put("type", type.name());
            object.put("name", ActionEx.getActionName(id));
            object.put("enabled", enabled);
            return object;
        }

        @Override
        public String toString() {
            return "(" + type + ", " + ActionEx.getActionName(id) + ", " + enabled + ")";
        }
    }
}
