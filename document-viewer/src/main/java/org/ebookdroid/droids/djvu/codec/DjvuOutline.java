package org.ebookdroid.droids.djvu.codec;

import org.ebookdroid.core.codec.OutlineLink;

import java.util.ArrayList;
import java.util.List;

public class DjvuOutline {

    private long docHandle;

    public List<OutlineLink> getOutline(final long dochandle) {
        final ArrayList<OutlineLink> ls = new ArrayList<OutlineLink>(20);  // this number seems reasonable
        docHandle = dochandle;
        final long expr = open(docHandle);
        ttOutline(ls, expr, 0);
        ls.trimToSize();
        return (List<OutlineLink>) ls;
    }

    private void ttOutline(final List<OutlineLink> ls, long expr, int level) {
        while (expConsp(expr)) {
            final String title = getTitle(expr);
            final String link = getLink(expr, docHandle);
            if (title != null) {
                ls.add(new OutlineLink(title, link, level));
            }
            final long child = getChild(expr);
            ttOutline(ls, child, level+1);

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
