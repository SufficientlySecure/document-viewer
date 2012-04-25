package org.ebookdroid.droids.fb2.codec;

import org.ebookdroid.droids.fb2.codec.FB2Document.ParsedContent;
import org.ebookdroid.droids.fb2.codec.RenderingStyle.Script;

import android.util.SparseArray;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.emdev.utils.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class FB2ContentHandler extends FB2BaseHandler {

    private boolean documentStarted = false, documentEnded = false;

    private boolean inSection = false;

    private boolean paragraphParsing = false;

    private boolean cover = false;

    private String tmpBinaryName = null;
    private boolean parsingNotes = false;
    private boolean parsingBinary = false;
    private boolean inTitle = false;
    private boolean inCite = false;
    private int noteId = -1;
    private boolean noteFirstWord = true;

    private boolean spaceNeeded = true;

    private static final Pattern notesPattern = Pattern.compile("n([0-9]+)|n_([0-9]+)|note_([0-9]+)|.*?([0-9]+)");
    private final StringBuilder tmpBinaryContents = new StringBuilder(64 * 1024);
    private final StringBuilder title = new StringBuilder();

    final SparseArray<FB2Words> words = new SparseArray<FB2Words>();

    int sectionLevel = -1;

    private boolean skipContent;

    public ParsedContent parsedContent;

    public FB2ContentHandler(final FB2Document fb2Document) {
        super(fb2Document);
        parsedContent = fb2Document.new ParsedContent();
        parsedContent.currentStream = null;
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
            throws SAXException {
        spaceNeeded = true;
        if ("p".equals(qName)) {
            paragraphParsing = true;
            if (!parsingNotes) {
                if (!inTitle) {
                    parsedContent.getMarkupStream().add(crs.paint.pOffset);
                }
            }
        } else if ("v".equals(qName)) {
            paragraphParsing = true;
            parsedContent.getMarkupStream().add(crs.paint.pOffset);
            parsedContent.getMarkupStream().add(crs.paint.vOffset);
        } else if ("binary".equals(qName)) {
            tmpBinaryName = attributes.getValue("id");
            tmpBinaryContents.setLength(0);
            parsingBinary = true;
        } else if ("body".equals(qName)) {
            if (!documentStarted && !documentEnded) {
                documentStarted = true;
                parsedContent.currentStream = null;
            }
            if ("notes".equals(attributes.getValue("name"))) {
                if (documentStarted) {
                    documentEnded = true;
                    parsedContent.getMarkupStream(null).add(new FB2MarkupEndDocument());
                }
                parsingNotes = true;
                crs = new RenderingStyle(FB2FontStyle.FOOTNOTE);
            }
        } else if ("section".equals(qName)) {
            if (parsingNotes) {
                parsedContent.currentStream = attributes.getValue("id");
                if (parsedContent.currentStream != null) {
                    final String n = getNoteId(parsedContent.currentStream, true);
                    parsedContent.getMarkupStream().add(text(n.toCharArray(), 0, n.length(), crs));
                    parsedContent.getMarkupStream().add(crs.paint.fixedSpace);
                }
            } else {
                inSection = true;
                sectionLevel++;
            }
        } else if ("title".equals(qName)) {
            if (!parsingNotes) {
                setTitleStyle(!inSection ? FB2FontStyle.MAIN_TITLE : FB2FontStyle.SECTION_TITLE);
                parsedContent.getMarkupStream().add(crs.jm);
                parsedContent.getMarkupStream().add(emptyLine(crs.textSize));
                parsedContent.getMarkupStream().add(FB2MarkupParagraphEnd.E);
                title.setLength(0);
            } else {
                skipContent = true;
            }
            inTitle = true;
        } else if ("cite".equals(qName)) {
            inCite = true;
            setEmphasisStyle();
            parsedContent.getMarkupStream().add(emptyLine(crs.textSize));
            parsedContent.getMarkupStream().add(FB2MarkupParagraphEnd.E);
        } else if ("subtitle".equals(qName)) {
            paragraphParsing = true;
            parsedContent.getMarkupStream().add(setSubtitleStyle().jm);
            parsedContent.getMarkupStream().add(emptyLine(crs.textSize));
            parsedContent.getMarkupStream().add(FB2MarkupParagraphEnd.E);
        } else if ("text-author".equals(qName) || "date".equals(qName)) {
            paragraphParsing = true;
            parsedContent.getMarkupStream().add(setTextAuthorStyle(inCite).jm);
            parsedContent.getMarkupStream().add(crs.paint.pOffset);
        } else if ("a".equals(qName)) {
            if (paragraphParsing) {
                if ("note".equalsIgnoreCase(attributes.getValue("type"))) {
                    String note = attributes.getValue("href");
                    parsedContent.getMarkupStream().add(new FB2MarkupNote(note));
                    String prettyNote = " " + getNoteId(note, false);
                    parsedContent.getMarkupStream().add(FB2MarkupNoSpace._instance);
                    parsedContent.getMarkupStream().add(
                            new FB2TextElement(prettyNote.toCharArray(), 0, prettyNote.length(), new RenderingStyle(
                                    crs, Script.SUPER)));
                    skipContent = true;
                }
            }
        } else if ("empty-line".equals(qName)) {
            parsedContent.getMarkupStream().add(emptyLine(crs.textSize));
            parsedContent.getMarkupStream().add(FB2MarkupParagraphEnd.E);
        } else if ("poem".equals(qName)) {
            parsedContent.getMarkupStream().add(FB2MarkupParagraphEnd.E);
            parsedContent.getMarkupStream().add(emptyLine(crs.textSize));
            parsedContent.getMarkupStream().add(setPoemStyle().jm);
        } else if ("strong".equals(qName)) {
            setBoldStyle();
        } else if ("sup".equals(qName)) {
            setSupStyle();
            spaceNeeded = false;
        } else if ("sub".equals(qName)) {
            setSubStyle();
            spaceNeeded = false;
        } else if ("strikethrough".equals(qName)) {
            setStrikeThrough();
        } else if ("emphasis".equals(qName)) {
            setEmphasisStyle();
        } else if ("epigraph".equals(qName)) {
            parsedContent.getMarkupStream().add(FB2MarkupParagraphEnd.E);
            parsedContent.getMarkupStream().add(setEpigraphStyle().jm);
        } else if ("image".equals(qName)) {
            final String ref = attributes.getValue("href");
            if (cover) {
                document.setCover(ref);
            } else {
                if (!paragraphParsing) {
                    parsedContent.getMarkupStream().add(emptyLine(crs.textSize));
                    parsedContent.getMarkupStream().add(FB2MarkupParagraphEnd.E);
                }
                parsedContent.getMarkupStream().add(new FB2MarkupImageRef(ref, paragraphParsing));
                if (!paragraphParsing) {
                    parsedContent.getMarkupStream().add(emptyLine(crs.textSize));
                    parsedContent.getMarkupStream().add(FB2MarkupParagraphEnd.E);
                }
            }
        } else if ("coverpage".equals(qName)) {
            cover = true;
        }
    }

    private FB2MarkupElement emptyLine(final int textSize) {
        return crs.paint.emptyLine;
    }

    private FB2TextElement text(final char[] ch, final int st, final int len, final RenderingStyle style) {
        FB2Words w = words.get(style.paint.key);
        if (w == null) {
            w = new FB2Words();
            words.append(style.paint.key, w);
        }
        return w.get(ch, st, len, style);
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        spaceNeeded = true;
        if ("p".equals(qName) || "v".equals(qName)) {
            if (!skipContent) {
                parsedContent.getMarkupStream().add(FB2MarkupParagraphEnd.E);
            }
            paragraphParsing = false;
        } else if ("binary".equals(qName)) {
            if (tmpBinaryContents.length() > 0) {
                document.addImage(tmpBinaryName, tmpBinaryContents.toString());
                tmpBinaryName = null;
                tmpBinaryContents.setLength(0);
            }
            parsingBinary = false;
        } else if ("body".equals(qName)) {
            parsingNotes = false;
            parsedContent.currentStream = null;
        } else if ("section".equals(qName)) {
            if (parsingNotes) {
                noteId = -1;
                noteFirstWord = true;
            } else {
                if (inSection) {
                    parsedContent.getMarkupStream().add(FB2MarkupEndPage.E);
                    sectionLevel--;
                    inSection = false;
                }
            }
        } else if ("title".equals(qName)) {
            inTitle = false;
            skipContent = false;
            if (!parsingNotes) {
                parsedContent.getMarkupStream().add(new FB2MarkupTitle(title.toString(), sectionLevel));
                parsedContent.getMarkupStream().add(emptyLine(crs.textSize));
                parsedContent.getMarkupStream().add(FB2MarkupParagraphEnd.E);
                parsedContent.getMarkupStream().add(setPrevStyle().jm);
            }
        } else if ("cite".equals(qName)) {
            inCite = false;
            parsedContent.getMarkupStream().add(emptyLine(crs.textSize));
            parsedContent.getMarkupStream().add(FB2MarkupParagraphEnd.E);
            parsedContent.getMarkupStream().add(setPrevStyle().jm);
        } else if ("subtitle".equals(qName)) {
            parsedContent.getMarkupStream().add(FB2MarkupParagraphEnd.E);
            parsedContent.getMarkupStream().add(emptyLine(crs.textSize));
            parsedContent.getMarkupStream().add(FB2MarkupParagraphEnd.E);
            parsedContent.getMarkupStream().add(setPrevStyle().jm);
            paragraphParsing = false;
        } else if ("text-author".equals(qName) || "date".equals(qName)) {
            parsedContent.getMarkupStream().add(FB2MarkupParagraphEnd.E);
            parsedContent.getMarkupStream().add(setPrevStyle().jm);
            paragraphParsing = false;
        } else if ("stanza".equals(qName)) {
            parsedContent.getMarkupStream().add(emptyLine(crs.textSize));
            parsedContent.getMarkupStream().add(FB2MarkupParagraphEnd.E);
        } else if ("poem".equals(qName)) {
            parsedContent.getMarkupStream().add(emptyLine(crs.textSize));
            parsedContent.getMarkupStream().add(FB2MarkupParagraphEnd.E);
            parsedContent.getMarkupStream().add(setPrevStyle().jm);
        } else if ("strong".equals(qName)) {
            setPrevStyle();
            spaceNeeded = false;
        } else if ("strikethrough".equals(qName)) {
            setPrevStyle();
        } else if ("sup".equals(qName)) {
            setPrevStyle();
            if (parsedContent.getMarkupStream().get(parsedContent.getMarkupStream().size() - 1) instanceof FB2MarkupNoSpace) {
                parsedContent.getMarkupStream().remove(parsedContent.getMarkupStream().size() - 1);
            }
        } else if ("sub".equals(qName)) {
            setPrevStyle();
            if (parsedContent.getMarkupStream().get(parsedContent.getMarkupStream().size() - 1) instanceof FB2MarkupNoSpace) {
                parsedContent.getMarkupStream().remove(parsedContent.getMarkupStream().size() - 1);
            }
        } else if ("emphasis".equals(qName)) {
            setPrevStyle();
            spaceNeeded = false;
        } else if ("epigraph".equals(qName)) {
            parsedContent.getMarkupStream().add(setPrevStyle().jm);
        } else if ("coverpage".equals(qName)) {
            cover = false;
        } else if ("a".equals(qName)) {
            if (paragraphParsing) {
                skipContent = false;
            }
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        if (skipContent
                || (!(documentStarted && !documentEnded) && !paragraphParsing && !parsingBinary && !parsingNotes)) {
            return;
        }
        if (parsingBinary) {
            tmpBinaryContents.append(ch, start, length);
        } else {
            if (inTitle) {
                title.append(ch, start, length);
            }
            final int count = StringUtils.split(ch, start, length, starts, lengths);

            if (count > 0) {
                if (!spaceNeeded && !Character.isWhitespace(ch[start])) {
                    parsedContent.getMarkupStream().add(FB2MarkupNoSpace._instance);
                }
                spaceNeeded = true;

                for (int i = 0; i < count; i++) {
                    final int st = starts[i];
                    final int len = lengths[i];
                    if (parsingNotes) {
                        if (noteFirstWord) {
                            noteFirstWord = false;
                            int id = -2;
                            try {
                                id = Integer.parseInt(new String(ch, st, len));
                            } catch (final Exception e) {
                                id = -2;
                            }
                            if (id == noteId) {
                                continue;
                            }
                        }
                    }
                    parsedContent.getMarkupStream().add(text(ch, st, len, crs));
                    if (crs.script != null) {
                        parsedContent.getMarkupStream().add(FB2MarkupNoSpace._instance);
                    }
                }
                if (Character.isWhitespace(ch[start + length - 1])) {
                    parsedContent.getMarkupStream().add(FB2MarkupNoSpace._instance);
                    parsedContent.getMarkupStream().add(crs.paint.space);
                }
                spaceNeeded = false;
            }
        }
    }

    private String getNoteId(String noteName, boolean bracket) {
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

}
