package org.ebookdroid.droids.fb2.codec.tags;


import org.emdev.common.xml.tags.XmlTag;

public enum FB2Tag {

    /** **/
    UNKNOWN("unknown", FB2TagId.UNKNOWN, true, false),
    /** **/
    P("p", FB2TagId.P, true, true, "id"),
    /** **/
    V("v", FB2TagId.V, true, true),
    /** **/
    SUBTITLE("subtitle", FB2TagId.SUBTITLE, true, true),
    /** **/
    TEXT_AUTHOR("text-author", FB2TagId.TEXT_AUTHOR, true, true),
    /** **/
    DATE("date", FB2TagId.DATE, true, true),
    /** **/
    CITE("cite", FB2TagId.CITE, true, true),
    /** **/
    SECTION("section", FB2TagId.SECTION, true, true, "id"),
    /** **/
    POEM("poem", FB2TagId.POEM, true, true),
    /** **/
    STANZA("stanza", FB2TagId.STANZA, true, true),
    /** **/
    EPIGRAPH("epigraph", FB2TagId.EPIGRAPH, true, true),
    /** **/
    ANNOTATION("annotation", FB2TagId.ANNOTATION, true, true),
    /** **/
    COVERPAGE("coverpage", FB2TagId.COVERPAGE, true, true),
    /** **/
    A("a", FB2TagId.A, true, true, "href", "type"),
    /** **/
    EMPTY_LINE("empty-line", FB2TagId.EMPTY_LINE, true, true),
    /** **/
    SUP("sup", FB2TagId.SUP, true, true),
    /** **/
    SUB("sub", FB2TagId.SUB, true, true),
    /** **/
    EMPHASIS("emphasis", FB2TagId.EMPHASIS, true, true),
    /** **/
    STRONG("strong", FB2TagId.STRONG, true, true),
    /** **/
    CODE("code", FB2TagId.CODE, true, true),
    /** **/
    STRIKETHROUGH("strikethrough", FB2TagId.STRIKETHROUGH, true, true),
    /** **/
    TITLE("title", FB2TagId.TITLE, true, true),
    /** **/
    TITLE_INFO("title-info", FB2TagId.TITLE_INFO, true, true),
    /** **/
    BODY("body", FB2TagId.BODY, true, true, "name"),
    /** **/
    IMAGE("image", FB2TagId.IMAGE, true, true, "href"),
    /** **/
    BINARY("binary", FB2TagId.BINARY, true, true, "id"),
    /** **/
    FICTIONBOOK("FictionBook", FB2TagId.FICTIONBOOK, true, true),
    /** **/
    BOOK_TITLE("book-title", FB2TagId.BOOK_TITLE, true, true),
    /** **/
    SEQUENCE("sequence", FB2TagId.SEQUENCE, true, true),
    /** **/
    FIRST_NAME("first-name", FB2TagId.FIRST_NAME, true, true),
    /** **/
    MIDDLE_NAME("middle-name", FB2TagId.MIDDLE_NAME, true, true),
    /** **/
    LAST_NAME("last-name", FB2TagId.LAST_NAME, true, true),
    /** **/
    AUTHOR("author", FB2TagId.AUTHOR, true, true),
    /** **/
    LANG("lang", FB2TagId.LANG, true, true),
    /** **/
    GENRE("genre", FB2TagId.GENRE, true, true),
    /** **/
    DESCRIPTION("description", FB2TagId.DESCRIPTION, true, true),
    /** **/
    TABLE("table", FB2TagId.TABLE, true, true),
    /** **/
    TR("tr", FB2TagId.TR, true, true),
    /** **/
    TD("td", FB2TagId.TD, true, true, "align"),
    /** **/
    TH("th", FB2TagId.TH, true, true, "align"),
    /** **/
    BR("br", FB2TagId.BR, true, true),
    /** **/
    UL("ul", FB2TagId.UL, true, true),
    /** **/
    LI("li", FB2TagId.LI, true, true)
    ;

    public final XmlTag tag;

    private FB2Tag(final String name, final byte tag, final boolean processChildren, final boolean processText,
            final String... attributes) {
        this.tag = FB2TagFactory.instance.tag(name, tag, processChildren, processText, attributes);
    }
}
