package org.ebookdroid.core;

import android.graphics.RectF;
import android.util.Log;

public class PageLink {

    private final int rect_type;
    private final int[] data;
    private final String url;

    PageLink(final String l, final int type, final int[] dt) {
        rect_type = type;
        data = dt;
        url = l;
    }

    public int getType() {
        return rect_type;
    }

    public RectF getRect() {
        return new RectF(data[0], data[1], data[2], data[3]);
    }

    public void debug() {
        Log.i("dddd", url);
        Log.i("dddd", rect_type + "   " + data.toString() + "   " + data.length);
        for (int i = 0; i < data.length; i++) {
            Log.i("dddd", "data[" + i + "]" + data[i]);
        }
    }

}
