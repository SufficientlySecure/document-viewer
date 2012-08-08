package org.ebookdroid.droids.fb2.codec;

import java.util.Arrays;
import java.util.HashMap;

final class FB2Tag {
	public static final byte UNKNOWN = 0;
	public static final byte P = 1;
	public static final byte V = 2;
	public static final byte SUBTITLE = 3;
	public static final byte TEXT_AUTHOR = 4;
	public static final byte DATE = 5;
	public static final byte CITE = 6;
	public static final byte SECTION = 7;
	public static final byte POEM = 8;
	public static final byte STANZA = 9;
	public static final byte EPIGRAPH = 10;
	public static final byte ANNOTATION = 11;
	public static final byte COVERPAGE = 12;
	public static final byte A = 13;
	public static final byte EMPTY_LINE = 14;
	public static final byte SUP = 15;
	public static final byte SUB = 16;
	public static final byte EMPHASIS = 17;
	public static final byte STRONG = 18;
	public static final byte CODE = 19;
	public static final byte STRIKETHROUGH = 20;
	public static final byte TITLE = 21;
	public static final byte BODY = 22;
	public static final byte IMAGE = 23;
	public static final byte BINARY = 24;
	public static final byte FICTIONBOOK = 25;

	public static final byte TITLE_INFO = 26;
	public static final byte BOOK_TITLE = 27;
	public static final byte AUTHOR = 28;
	public static final byte LANG = 29;
	public static final byte FIRST_NAME = 30;
	public static final byte MIDDLE_NAME = 31;
	public static final byte LAST_NAME = 32;
	public static final byte SEQUENCE = 33;
	public static final byte GENRE = 34;

	public static final byte DESCRIPTION = 35;

	public static final byte TABLE = 36;
    public static final byte TR = 37;
    public static final byte TD = 38;
    public static final byte TH = 39;

    public static final FB2Tag unknownTag;

    private static final HashMap<String, FB2Tag> tagsByName = new HashMap<String, FB2Tag>(256, 0.2f);

	static {
        unknownTag = addTag("unknown", UNKNOWN, true, false);

        addTag("p", P, true, true);
        addTag("v", V, true, true);
        addTag("subtitle", SUBTITLE, true, true);
        addTag("text-author", TEXT_AUTHOR, true, true);
        addTag("date", DATE, true, true);
        addTag("cite", CITE, true, true);
        addTag("section", SECTION, true, true, "id");
        addTag("poem", POEM, true, true);
        addTag("stanza", STANZA, true, true);
        addTag("epigraph", EPIGRAPH, true, true);
        addTag("annotation", ANNOTATION, true, true);
        addTag("coverpage", COVERPAGE, true, true);
        addTag("a", A, true, true, "type", "href");
        addTag("empty-line", EMPTY_LINE, true, true);
        addTag("sup", SUP, true, true);
        addTag("sub", SUB, true, true);
        addTag("emphasis", EMPHASIS, true, true);
        addTag("strong", STRONG, true, true);
        addTag("code", CODE, true, true);
        addTag("strikethrough", STRIKETHROUGH, true, true);
        addTag("title", TITLE, true, true);
        addTag("title-info", TITLE_INFO, true, true);
        addTag("body", BODY, true, true, "name");
        addTag("image", IMAGE, true, true, "href");
        addTag("binary", BINARY, true, true, "id");
        addTag("fictionbook", FICTIONBOOK, true, true);
        addTag("book-title", BOOK_TITLE, true, true);
        addTag("sequence", SEQUENCE, true, true);
        addTag("first-name", FIRST_NAME, true, true);
        addTag("middle-name", MIDDLE_NAME, true, true);
        addTag("last-name", LAST_NAME, true, true);
        addTag("book-title", BOOK_TITLE, true, true);
        addTag("author", AUTHOR, true, true);
        addTag("lang", LANG, true, true);
        addTag("genre", GENRE, true, true);
        addTag("description", DESCRIPTION, true, true);
        addTag("table", TABLE, true, true);
        addTag("tr", TR, true, true);
        addTag("td", TD, true, true, "align");
        addTag("th", TH, true, true, "align");
    }

    public final byte tag;
    public final String name;
    public final boolean processChildren;
    public final boolean processText;
    public final String[] attributes;

    private FB2Tag(byte tag, String name, boolean processChildren, boolean processText, String[] attributes) {
        super();
        this.tag = tag;
        this.name = name;
        this.processChildren = processChildren;
        this.processText = processText;
        this.attributes = attributes;

        if (this.attributes.length > 1) {
            Arrays.sort(this.attributes);
        }
    }

    public String toString() {
        return name;
    }

    public static byte getTagIdByName(String name) {
        FB2Tag tag = tagsByName.get(name);
        if (tag == null) {
            final String upperCaseName = name.toLowerCase().intern();
            tag = tagsByName.get(upperCaseName);
            if (tag == null) {
                tag = unknownTag;
                tagsByName.put(upperCaseName, tag);
            }
            tagsByName.put(name, tag);
        }
        return tag.tag;
	}

    public static FB2Tag getTagByName(String name) {
        FB2Tag tag = tagsByName.get(name);
        if (tag == null) {
			final String upperCaseName = name.toLowerCase().intern();
            tag = tagsByName.get(upperCaseName);
            if (tag == null) {
                tag = unknownTag;
                tagsByName.put(upperCaseName, tag);
			}
            tagsByName.put(name, tag);
		}
        return tag;
	}

    private static FB2Tag addTag(String name, byte tag, boolean processChildren, boolean processText,
            String... attributes) {
        FB2Tag t = new FB2Tag(tag, name, processChildren, processText, attributes);
        tagsByName.put(name, t);
        return t;
	}
}
