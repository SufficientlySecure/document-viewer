package org.ebookdroid.droids.mupdf.codec;

import org.ebookdroid.core.codec.PageLink;

import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;

public class MuPdfLinks {

    private static final int FZ_LINK_NONE = 0;
    private static final int FZ_LINK_GOTO = 1;
    private static final int FZ_LINK_URI = 2;
    private static final int FZ_LINK_LAUNCH = 3;
    private static final int FZ_LINK_NAMED = 4;
    private static final int FZ_LINK_GOTOR = 5;

    private static final float[] temp = new float[4];

    static List<PageLink> getPageLinks(final long docHandle, final long pageHandle, final RectF pageBounds) {
        final List<PageLink> links = new ArrayList<PageLink>();
        for (long linkHandle = getFirstPageLink(docHandle, pageHandle); linkHandle != 0; linkHandle = getNextPageLink(linkHandle)) {
            final PageLink link = new PageLink();
            final int type = getPageLinkType(linkHandle);
            if (type == FZ_LINK_URI) {
                link.url = getPageLinkUrl(linkHandle);
                links.add(link);
            } else if (type == FZ_LINK_GOTO) {
                link.rectType = 1;
                if (fillPageLinkSourceRect(linkHandle, temp)) {
                    link.sourceRect = new RectF();
                    link.sourceRect.left = (temp[0] - pageBounds.left) / pageBounds.width();
                    link.sourceRect.top = (temp[1] - pageBounds.top) / pageBounds.height();
                    link.sourceRect.right = (temp[2] - pageBounds.left) / pageBounds.width();
                    link.sourceRect.bottom = (temp[3] - pageBounds.top) / pageBounds.height();
                }

                link.targetPage = getPageLinkTargetPage(linkHandle);
                if (link.targetPage > 0 && fillPageLinkTargetPoint(linkHandle, temp)) {
                    link.targetRect = new RectF();
                    link.targetRect.left = temp[0];
                    link.targetRect.top = temp[1];

                    MuPdfDocument.normalizeLinkTargetRect(docHandle, link.targetPage, link.targetRect);
                }

                links.add(link);
            }
        }

        return links;

    }

    private static native long getFirstPageLink(long dochandle, long pagehandle);

    private static native long getNextPageLink(long linkhandle);

    private static native int getPageLinkType(long linkhandle);

    private static native String getPageLinkUrl(long linkhandle);

    private static native boolean fillPageLinkSourceRect(long linkhandle, float[] bounds);

    private static native int getPageLinkTargetPage(long linkhandle);

    private static native boolean fillPageLinkTargetPoint(long linkhandle, float[] point);

}
