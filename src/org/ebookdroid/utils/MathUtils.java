package org.ebookdroid.utils;

import android.graphics.Rect;
import android.graphics.RectF;

public class MathUtils {

    public static int adjust(final int value, final int min, final int max) {
        return Math.min(Math.max(min, value), max);
    }

    public static Rect rect(final RectF rect) {
        return new Rect((int) rect.left, (int) rect.top, (int) rect.right, (int) rect.bottom);
    }

    public static RectF zoom(final RectF rect, float zoom) {
        return new RectF(zoom *  rect.left, zoom * rect.top, zoom * rect.right, zoom * rect.bottom);
    }

    public static RectF zoom(final Rect rect, float zoom) {
        return new RectF(zoom *  rect.left, zoom * rect.top, zoom * rect.right, zoom * rect.bottom);
    }

    public static Rect zoom(final float left, final float top, final float right, final float bottom, float zoom) {
        return new Rect((int)(zoom * left), (int)(zoom * top), (int)(zoom * right), (int)(zoom * bottom));
    }

    public static int min(int... values) {
        int min = Integer.MAX_VALUE;
        for(int v : values) {
            min = Math.min(v, min);
        }
        return min;
    }

    public static int max(int... values) {
        int max = Integer.MIN_VALUE;
        for(int v : values) {
            max = Math.max(v, max);
        }
        return max;
    }

}
