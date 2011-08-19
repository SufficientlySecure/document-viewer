package org.ebookdroid.core;

import org.ebookdroid.core.log.LogContext;

import android.graphics.RectF;

public class PageLink {

    private static final LogContext LCTX = LogContext.ROOT.lctx("dddd");

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
        if (LCTX.isDebugEnabled()) {
            LCTX.d(url);
            LCTX.d(rect_type + "   " + data.toString() + "   " + data.length);
            for (int i = 0; i < data.length; i++) {
                LCTX.d("data[" + i + "]" + data[i]);
            }
        }
    }

}
