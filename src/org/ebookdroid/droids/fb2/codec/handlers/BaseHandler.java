package org.ebookdroid.droids.fb2.codec.handlers;

import org.ebookdroid.droids.fb2.codec.ParsedContent;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.emdev.common.fonts.data.FontStyle;
import org.emdev.common.textmarkup.JustificationMode;
import org.emdev.common.textmarkup.MarkupElement;
import org.emdev.common.textmarkup.RenderingStyle;
import org.emdev.common.textmarkup.TextStyle;

public abstract class BaseHandler {

    protected static final Pattern notesPattern = Pattern.compile("n([0-9]+)|n_([0-9]+)|note_([0-9]+)|.*?([0-9]+)");

    public final ParsedContent parsedContent;

    protected final int[] starts = new int[10000];

    protected final int[] lengths = new int[10000];

    protected RenderingStyle crs;

    protected final LinkedList<RenderingStyle> renderingStates = new LinkedList<RenderingStyle>();

    protected String currentStream = null;
    protected String oldStream = null;

    protected int noteId = -1;

    public BaseHandler(final ParsedContent content) {
        parsedContent = content;
        currentStream = null;
        crs = new RenderingStyle(content, TextStyle.TEXT);
    }

    protected final RenderingStyle setPrevStyle() {
        if (!renderingStates.isEmpty()) {
            crs = renderingStates.removeFirst();
        }
        return crs;
    }

    protected final RenderingStyle setTitleStyle(final TextStyle font) {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(parsedContent, crs, font, JustificationMode.Center);
        return crs;
    }

    protected final RenderingStyle setEpigraphStyle() {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(parsedContent, crs, JustificationMode.Right,
                org.emdev.common.fonts.data.FontStyle.ITALIC);
        return crs;
    }

    protected final RenderingStyle setBoldStyle() {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(parsedContent, crs, true);
        return crs;
    }

    protected final RenderingStyle setSupStyle() {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(parsedContent, crs, RenderingStyle.Script.SUPER);
        return crs;
    }

    protected final RenderingStyle setSubStyle() {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(parsedContent, crs, RenderingStyle.Script.SUB);
        return crs;
    }

    protected final RenderingStyle setStrikeThrough() {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(parsedContent, crs, RenderingStyle.Strike.THROUGH);
        return crs;
    }

    protected final RenderingStyle setEmphasisStyle() {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(parsedContent, crs, FontStyle.ITALIC);
        return crs;
    }

    protected final RenderingStyle setSubtitleStyle() {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(parsedContent, crs, TextStyle.SUBTITLE, JustificationMode.Center, FontStyle.BOLD);
        return crs;
    }

    protected final RenderingStyle setTextAuthorStyle(final boolean italic) {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(parsedContent, crs, JustificationMode.Right, italic ? FontStyle.ITALIC
                : FontStyle.REGULAR);
        return crs;
    }

    protected final RenderingStyle setPoemStyle() {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(parsedContent, crs, JustificationMode.Left, FontStyle.ITALIC);
        return crs;
    }

    protected final RenderingStyle setPreformatted() {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(parsedContent, parsedContent.mono, TextStyle.PREFORMATTED);
        return crs;
    }

    protected MarkupElement emptyLine(final int textSize) {
        return crs.paint.emptyLine;
    }

    protected String getNoteId(final String noteName, final boolean bracket) {
        final Matcher matcher = notesPattern.matcher(noteName);
        String n = noteName;
        if (matcher.matches()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (matcher.group(i) != null) {
                    noteId = Integer.parseInt(matcher.group(i));
                    n = "" + noteId + (bracket ? ")" : "");
                    break;
                }
                noteId = -1;
            }
        }
        return n;
    }

    protected int getNoteId(final char[] ch, final int st, final int len) {
        int id = -2;
        try {
            int last = len - 1;
            final char lc = ch[st + last];
            if (lc == '.' || lc == ')') {
                last--;
            }
            final String fw = new String(ch, st, last + 1);
            id = Integer.parseInt(fw);
        } catch (final Exception e) {
            id = -2;
        }
        return id;
    }
}
