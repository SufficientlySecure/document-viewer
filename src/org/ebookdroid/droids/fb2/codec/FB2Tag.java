package org.ebookdroid.droids.fb2.codec;

import java.util.Arrays;
import java.util.HashMap;

import org.emdev.utils.collections.SymbolTree;

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

    private static final HashMap<String, FB2Tag> tagsByName = new HashMap<String, FB2Tag>(256, 0.2f);
    private static final SymbolTree<FB2Tag> tagsTree = new SymbolTree<FB2Tag>();

    static {
        for (final FB2Tag t : values()) {
            tagsByName.put(t.name, t);
            tagsTree.add(t, t.name);
        }
    }

    public final byte tag;
    public final String name;
    public final char[] _name;
    public final boolean processChildren;
    public final boolean processText;
    public final String[] attributes;

    private FB2Tag(final String name, final byte tag, final boolean processChildren, final boolean processText,
            final String... attributes) {
        this.tag = tag;
        this.name = name;
        this._name = name.toCharArray();
        this.processChildren = processChildren;
        this.processText = processText;
        this.attributes = attributes;

        if (this.attributes.length > 1) {
            Arrays.sort(this.attributes);
        }
    }

    @Override
    public String toString() {
        return name;
    }

    public static FB2Tag getTagByName(final String name) {
        FB2Tag tag = tagsByName.get(name);
        if (tag == null) {
            final String upperCaseName = name.toLowerCase().intern();
            tag = tagsByName.get(upperCaseName);
            if (tag == null) {
                tag = UNKNOWN;
                tagsByName.put(upperCaseName, tag);
            }
            tagsByName.put(name, tag);
        }
        return tag;
    }

    public static FB2Tag getTagByName1(final char[] ch, final int start, final int length) {
        switch (length) {
            case 1:
                switch (ch[start]) {
                    case 'p':
                        return P;
                    case 'v':
                        return V;
                    case 'a':
                        return A;
                }
                return UNKNOWN;
            case 2:
                switch (ch[start]) {
                    case 't':
                        switch (ch[start + 1]) {
                            case 'r':
                                return TR;
                            case 'd':
                                return TD;
                            case 'h':
                                return TH;
                        }
                        return UNKNOWN;
                    case 'b':
                        switch (ch[start + 1]) {
                            case 'r':
                                return BR;
                        }
                        return UNKNOWN;
                    case 'u':
                        switch (ch[start + 1]) {
                            case 'l':
                                return UL;
                        }
                        return UNKNOWN;
                    case 'l':
                        switch (ch[start + 1]) {
                            case 'i':
                                return LI;
                        }
                        return UNKNOWN;
                }
                return UNKNOWN;
            case 3:
                if (ch[start] == 's' && ch[start + 1] == 'u') {
                    if (ch[start + 2] == 'p') {
                        return SUP;
                    } else if (ch[start + 2] == 'b') {
                        return SUB;
                    }
                }
                return UNKNOWN;
            default:
                L1: for (final FB2Tag t : values()) {
                    if (t._name.length == length) {
                        for (int i = 0; i < length; i++) {
                            if (t._name[i] != ch[start + i]) {
                                continue L1;
                            }
                        }
                        return t;
                    }
                }
            case 0:
                return UNKNOWN;
        }
    }

    public static FB2Tag getTagByName2(final char[] ch, final int start, final int length) {
        final FB2Tag fb2Tag = tagsTree.get(ch, start, length);
        return fb2Tag != null ? fb2Tag : UNKNOWN;
    }

    public static FB2Tag getTagByName3(final char[] ch, final int start, final int length) {
        switch (length) {
            case 1:
                switch (ch[start]) {
                    case 'p':
                        return P;
                    case 'v':
                        return V;
                    case 'a':
                        return A;
                }
                return UNKNOWN;
            case 2:
                switch (ch[start]) {
                    case 't':
                        switch (ch[start + 1]) {
                            case 'r':
                                return TR;
                            case 'd':
                                return TD;
                            case 'h':
                                return TH;
                        }
                        return UNKNOWN;
                    case 'b':
                        switch (ch[start + 1]) {
                            case 'r':
                                return BR;
                        }
                        return UNKNOWN;
                    case 'u':
                        switch (ch[start + 1]) {
                            case 'l':
                                return UL;
                        }
                        return UNKNOWN;
                    case 'l':
                        switch (ch[start + 1]) {
                            case 'i':
                                return LI;
                        }
                        return UNKNOWN;
                }
                return UNKNOWN;
            case 3:
                if (ch[start] == 's' && ch[start + 1] == 'u') {
                    if (ch[start + 2] == 'p') {
                        return SUP;
                    } else if (ch[start + 2] == 'b') {
                        return SUB;
                    }
                }
                return UNKNOWN;
            default:
                final FB2Tag t = tagsTree.get(ch, start, length);
                return t != null ? t : UNKNOWN;
            case 0:
                return UNKNOWN;
        }
    }

}
