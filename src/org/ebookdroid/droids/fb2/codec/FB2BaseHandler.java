package org.ebookdroid.droids.fb2.codec;

import java.util.LinkedList;

import org.xml.sax.helpers.DefaultHandler;

public class FB2BaseHandler extends DefaultHandler {

    protected final FB2Document document;

    protected final int[] starts = new int[10000];

    protected final int[] lengths = new int[10000];

    protected RenderingStyle crs = new RenderingStyle(FB2FontStyle.TEXT);

    private final LinkedList<RenderingStyle> renderingStates = new LinkedList<RenderingStyle>();

    public FB2BaseHandler(final FB2Document fb2Document) {
        this.document = fb2Document;
    }

    protected final RenderingStyle setPrevStyle() {
        if (!renderingStates.isEmpty()) {
            crs = renderingStates.removeFirst();
        }
        return crs;
    }

    protected final RenderingStyle setTitleStyle(final FB2FontStyle font) {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(crs, font, JustificationMode.Center);
        return crs;
    }

    protected final RenderingStyle setEpigraphStyle() {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(crs, JustificationMode.Right, RenderingStyle.ITALIC_TF);
        return crs;
    }

    protected final RenderingStyle setBoldStyle() {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(crs, true);
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
        crs = new RenderingStyle(crs, RenderingStyle.ITALIC_TF);
        return crs;
    }

    protected final RenderingStyle setSubtitleStyle() {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(crs, FB2FontStyle.SUBTITLE, JustificationMode.Center, true,
                RenderingStyle.NORMAL_TF);
        return crs;
    }

    protected final RenderingStyle setTextAuthorStyle(final boolean italic) {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(crs, FB2FontStyle.TEXT, JustificationMode.Right, false,
                italic ? RenderingStyle.ITALIC_TF : RenderingStyle.NORMAL_TF);
        return crs;
    }

    protected final RenderingStyle setPoemStyle() {
        renderingStates.addFirst(crs);
        crs = new RenderingStyle(crs, FB2FontStyle.TEXT, JustificationMode.Left, false, RenderingStyle.ITALIC_TF);
        return crs;
    }
}
