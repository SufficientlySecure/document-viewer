package org.ebookdroid.pdfdroid.codec;

import org.ebookdroid.core.OutlineLink;

import java.util.ArrayList;
import java.util.List;

public class PdfOutline {

    private long docHandle;

    public List<OutlineLink> getOutline(final long dochandle) {
        final List<OutlineLink> ls = new ArrayList<OutlineLink>();
        docHandle = dochandle;
        final long outline = open(dochandle);
        ttOutline(ls, outline, 0);
        free(dochandle);
        return ls;
    }

    private void ttOutline(final List<OutlineLink> ls, long outline, int level) {
        while (outline > 0) {

            final String title = getTitle(outline);
            final String link = getLink(outline, docHandle);
            if (title != null) {
                ls.add(new OutlineLink(title, link, level));
            }

            final long child = getChild(outline);
            ttOutline(ls, child, level + 1);

            outline = getNext(outline);
        }
    }

    private static native String getTitle(long outlinehandle);

    private static native String getLink(long outlinehandle, long dochandle);

    private static native long getNext(long outlinehandle);

    private static native long getChild(long outlinehandle);

    private static native long open(long dochandle);

    private static native void free(long dochandle);
}
