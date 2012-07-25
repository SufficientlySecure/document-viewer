package org.ebookdroid.droids.fb2.codec;

import java.util.LinkedList;

import org.emdev.common.fonts.data.FontStyle;
import org.emdev.common.textmarkup.TextStyle;
import org.emdev.common.textmarkup.JustificationMode;
import org.emdev.common.textmarkup.RenderingStyle;
import org.xml.sax.helpers.DefaultHandler;

public class FB2BaseHandler extends DefaultHandler {

    protected final int[] starts = new int[10000];

    protected final int[] lengths = new int[10000];

    protected RenderingStyle crs;

    private final LinkedList<RenderingStyle> renderingStates = new LinkedList<RenderingStyle>();

    public final ParsedContent parsedContent;
    String currentStream = null;
    String oldStream = null;

    public FB2BaseHandler(ParsedContent content) {
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
        crs = new RenderingStyle(crs, font, JustificationMode.Center);
        return crs;
    }

    protected final RenderingStyle setEpigraphStyle() {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(parsedContent, crs, JustificationMode.Right, org.emdev.common.fonts.data.FontStyle.ITALIC);
        return crs;
    }

    protected final RenderingStyle setBoldStyle() {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(parsedContent, crs, true);
        return crs;
    }

    protected final RenderingStyle setSupStyle() {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(crs, RenderingStyle.Script.SUPER);
        return crs;
    }

    protected final RenderingStyle setSubStyle() {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(crs, RenderingStyle.Script.SUB);
        return crs;
    }

    protected final RenderingStyle setStrikeThrough() {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(crs, RenderingStyle.Strike.THROUGH);
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
        crs = new RenderingStyle(parsedContent, crs, TextStyle.TEXT, JustificationMode.Right,italic ? FontStyle.ITALIC : FontStyle.REGULAR);
        return crs;
    }

    protected final RenderingStyle setPoemStyle() {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(parsedContent, crs, TextStyle.TEXT, JustificationMode.Left, FontStyle.ITALIC);
        return crs;
    }
}
