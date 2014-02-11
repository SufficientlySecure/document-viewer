package org.ebookdroid.opds.xml;


import org.emdev.common.xml.tags.XmlTag;

public enum OPDSTag {

    /** **/
    UNKNOWN("unknown", OPDSTagId.UNKNOWN, true, false),
    /** **/
    CONTENT("content", OPDSTagId.CONTENT, true, true, "type"),
    /** **/
    LINK("link", OPDSTagId.LINK, true, true,  "href", "rel", "title", "type"),
    /** **/
    ENTRY("entry", OPDSTagId.ENTRY, true, true),
    /** **/
    ID("id", OPDSTagId.ID, true, true),
    /** **/
    TITLE("title", OPDSTagId.TITLE, true, true);

    public final XmlTag tag;

    private OPDSTag(final String name, final byte tag, final boolean processChildren, final boolean processText,
            final String... attributes) {
        this.tag = OPDSTagFactory.instance.tag(name, tag, processChildren, processText, attributes);
    }
}
