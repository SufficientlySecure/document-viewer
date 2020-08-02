package org.ebookdroid.droids.mupdf.codec;

import org.ebookdroid.core.codec.OutlineLink;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Outline;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import android.graphics.RectF;

public class MuPdfOutline {

    private final static Pattern regex = Pattern.compile("\\d+");

    public static List<OutlineLink> getOutline(final MuPdfDocument doc) {
        final Outline[] ol = doc.documentHandle.loadOutline();
        final int allocCount = ol != null ? 2*ol.length : 0;
        final List<OutlineLink> ls = new ArrayList<>(allocCount);
        if (ol != null) {
            for (Outline o : ol) {
                ttOutline(doc, o, ls, 0);
            }
        }
        return ls;
    }

    private static void ttOutline(final MuPdfDocument doc, final Outline ol,
                                  final List<OutlineLink> list, int level) {
        final Scanner s = new Scanner(ol.uri);
        final String page = s.findInLine(regex);
        final String x = s.findInLine(regex);
        final String y = s.findInLine(regex);
        s.close();
        OutlineLink link = new OutlineLink(ol.title, "#" + page, level);
        link.targetRect = new RectF();
        link.targetRect.left = x != null ? Float.parseFloat(x) : 0;
        link.targetRect.top = y != null ? Float.parseFloat(y) : 0;
        doc.normalizeLinkTargetRect(Integer.parseInt(page), link.targetRect);
        list.add(link);
        if (ol.down != null) {
            for (Outline o : ol.down) {
                ttOutline(doc, o, list, level++);
            }
        }
    }
}
