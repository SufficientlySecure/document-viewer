package org.ebookdroid.core.touch;

import org.ebookdroid.core.settings.AppSettings;

import android.graphics.Rect;
import android.graphics.RectF;

import java.util.LinkedList;

public class TouchManager {

    private static final TouchManager instance = new TouchManager();

    private final LinkedList<Region> regions = new LinkedList<Region>();

    public static TouchManager getInstance() {
        return instance;
    }

    public static void applyOldStyleSettings(final AppSettings newSettings) {
        instance.clear();

        {
            final Region r = instance.addRegion(0, 0, 100, 100);
            r.setAction(Touch.DoubleTap, newSettings.getZoomByDoubleTap(), "toggleZoomControls");
        }
        {
            final Region r = instance.addRegion(0, 0, 100, newSettings.getTapSize());
            r.setAction(Touch.SingleTap, newSettings.getTapScroll(), "verticalConfigScrollUp");
        }
        {
            final Region r = instance.addRegion(0, 100 - newSettings.getTapSize(), 100, 100);
            r.setAction(Touch.SingleTap, newSettings.getTapScroll(), "verticalConfigScrollDown");
        }
    }

    public void clear() {
        regions.clear();
    }

    public String getAction(final Touch type, final float x, final float y, final float width, final float height) {
        for (final Region r : regions) {
            final RectF rect = r.getActualRect(width, height);
            if (rect.left <= x && x < rect.right && rect.top <= y && y < rect.bottom) {
                final ActionRef action = r.getAction(type);
                if (action != null && action.enabled) {
                    return action.name;
                }
            }
        }
        return null;
    }

    public Region addRegion(final int left, final int top, final int right, final int bottom) {
        final Region r = new Region(new Rect(left, top, right, bottom));
        regions.addFirst(r);
        return r;
    }

    public void removeRegion(final Region r) {
        regions.remove(r);
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

        public ActionRef setAction(final Touch type, final boolean enabled, final String name) {
            final ActionRef a = new ActionRef(type, enabled, name);
            actions[type.ordinal()] = a;
            return a;
        }

        public RectF getActualRect(final float width, final float height) {
            return new RectF(width * rect.left / 100.0f, height * rect.top / 100.0f, width * rect.right / 100.0f,
                    height * rect.bottom / 100.0f);
        }

    }

    public static class ActionRef {

        public final Touch type;
        public final String name;
        public boolean enabled;

        public ActionRef(final Touch type, final boolean enabled, final String name) {
            this.type = type;
            this.enabled = enabled;
            this.name = name;
        }
    }
}
