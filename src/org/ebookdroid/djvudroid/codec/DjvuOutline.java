package org.ebookdroid.djvudroid.codec;

import org.ebookdroid.core.OutlineLink;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DjvuOutline {

    private long docHandle;

    public List<OutlineLink> getOutline(final long dochandle) {
        final List<OutlineLink> ls = new ArrayList<OutlineLink>();
        docHandle = dochandle;
        final long expr = open(docHandle);
        ttOutline(ls, expr);
        return ls;
    }

    private void ttOutline(final List<OutlineLink> ls, long expr) {
        while (expConsp(expr)) {
            final String title = getTitle(expr);
            final String link = getLink(expr, docHandle);
            if (title != null) {
                Log.d("DjvuOutline", title);
                ls.add(new OutlineLink(title, link));
            }
            final long child = getChild(expr);
            ttOutline(ls, child);

            expr = getNext(expr);
        }

    }

    private static native long open(long dochandle);

    private static native boolean expConsp(long expr);

    private static native String getTitle(long expr);

    private static native String getLink(long expr, long dochandle);

    private static native long getNext(long expr);

    private static native long getChild(long expr);

}
