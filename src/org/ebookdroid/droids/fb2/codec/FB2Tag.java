package org.ebookdroid.droids.fb2.codec;

import java.util.HashMap;
import java.util.Map;

public enum FB2Tag {

    /**
     *
     */
    BODY("body"),
    /**
     *
     */
    SECTION("section"),
    /**
     *
     */
    TITLE("title"),
    /**
     *
     */
    EPIGRAPH("epigraph"),
    /**
     *
     */
    PARAGRAPH("p"),
    /**
     *
     */
    AHREF("a"),
    /**
     *
     */
    IMAGE("image"),
    /**
     *
     */
    COVERPAGE("coverpage"),
    /**
     *
     */
    EMPTY_LINE("empty-line"),
    /**
     *
     */
    BINARY("binary"),
    /**
     *
     */
    STRONG("strong"),
    /**
     *
     */
    EMPHASIS("emphasis"),
    /**
    *
    */
    CITE("cite");

    private static final Map<String, FB2Tag> tags;

    private String tag;

    static {
        tags = new HashMap<String, FB2Tag>();
        for (final FB2Tag tag : values()) {
            tags.put(tag.getTag(), tag);
        }
    }

    private FB2Tag(final String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }

    public static FB2Tag getTag(final String tag) {
        return tags.get(tag.toLowerCase());
    }
}
