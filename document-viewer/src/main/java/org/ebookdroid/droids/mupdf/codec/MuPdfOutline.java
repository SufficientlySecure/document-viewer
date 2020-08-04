package org.ebookdroid.droids.mupdf.codec;

import org.ebookdroid.core.codec.OutlineLink;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Outline;

import java.util.ArrayList;
import java.util.List;

import android.graphics.RectF;

public class MuPdfOutline {

    public static List<OutlineLink> getOutline(final MuPdfDocument doc) {
        final Outline[] ol = doc.documentHandle.loadOutline();
        final int allocCount = ol != null ? 5 * ol.length : 0;
        final ArrayList<OutlineLink> ls = new ArrayList<>(allocCount);
        if (ol != null) {
            for (Outline o : ol) {
                ttOutline(doc, o, ls, 0);
            }
        }
        ls.trimToSize();
        return (List<OutlineLink>) ls;
    }

    private static void ttOutline(final MuPdfDocument doc, final Outline ol,
                                  final List<OutlineLink> list, int level) {
        final String[] parts = ol.uri.split(",");
        OutlineLink link = new OutlineLink(ol.title, parts[0], level);
        link.targetRect = new RectF();
        if (parts.length >= 3) {
            link.targetRect.left = Float.parseFloat(parts[1]);
            link.targetRect.top = Float.parseFloat(parts[2]);
        }
        doc.normalizeLinkTargetRect(Integer.parseInt(parts[0].substring(1)), link.targetRect);
        list.add(link);
        if (ol.down != null) {
            for (Outline o : ol.down) {
                ttOutline(doc, o, list, level++);
            }
        }
    }
}
