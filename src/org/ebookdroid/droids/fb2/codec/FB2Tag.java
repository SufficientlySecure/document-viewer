package org.ebookdroid.droids.fb2.codec;

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


	private static final HashMap<String, Byte> tagsByName = new HashMap<String, Byte>(256, 0.2f);
	private static final Byte unknownTag;

	static {
		tagsByName.put("unknown", UNKNOWN);
		unknownTag = (Byte)tagsByName.get("unknown");
		tagsByName.put("p", P);
		tagsByName.put("v", V);
		tagsByName.put("subtitle", SUBTITLE);
		tagsByName.put("text-author", TEXT_AUTHOR);
		tagsByName.put("date", DATE);
		tagsByName.put("cite", CITE);
		tagsByName.put("section", SECTION);
		tagsByName.put("poem", POEM);
		tagsByName.put("stanza", STANZA);
		tagsByName.put("epigraph", EPIGRAPH);
		tagsByName.put("annotation", ANNOTATION);
		tagsByName.put("coverpage", COVERPAGE);
		tagsByName.put("a", A);
		tagsByName.put("empty-line", EMPTY_LINE);
		tagsByName.put("sup", SUP);
		tagsByName.put("sub", SUB);
		tagsByName.put("emphasis", EMPHASIS);
		tagsByName.put("strong", STRONG);
		tagsByName.put("code", CODE);
		tagsByName.put("strikethrough", STRIKETHROUGH);
		tagsByName.put("title", TITLE);
		tagsByName.put("title-info", TITLE_INFO);
		tagsByName.put("body", BODY);
		tagsByName.put("image", IMAGE);
		tagsByName.put("binary", BINARY);
		tagsByName.put("fictionbook", FICTIONBOOK);
		tagsByName.put("book-title", BOOK_TITLE);
		tagsByName.put("sequence", SEQUENCE);
		tagsByName.put("first-name", FIRST_NAME);
		tagsByName.put("middle-name", MIDDLE_NAME);
		tagsByName.put("last-name", LAST_NAME);
		tagsByName.put("book-title", BOOK_TITLE);
		tagsByName.put("author", AUTHOR);
		tagsByName.put("lang", LANG);
		tagsByName.put("genre", GENRE);
		tagsByName.put("description", DESCRIPTION);
        tagsByName.put("table", TABLE);
        tagsByName.put("tr", TR);
        tagsByName.put("td", TD);
        tagsByName.put("th", TH);
	}

	public static byte getTagByName(String name) {
		final HashMap<String,Byte> tagByName = tagsByName;
		Byte num = tagByName.get(name);
		if (num == null) {
			final String upperCaseName = name.toLowerCase().intern();
			num = (Byte)tagByName.get(upperCaseName);
			if (num == null) {
				num = unknownTag;
				tagByName.put(upperCaseName, num);
			}
			tagByName.put(name, num);
		}
		return num.byteValue();
	}

	private FB2Tag() {
	}
}
