package org.ebookdroid.droids.fb2.codec;


import android.util.SparseArray;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.emdev.utils.StringUtils;
import org.emdev.utils.textmarkup.FontStyle;
import org.emdev.utils.textmarkup.MarkupEndDocument;
import org.emdev.utils.textmarkup.MarkupEndPage;
import org.emdev.utils.textmarkup.MarkupImageRef;
import org.emdev.utils.textmarkup.MarkupNoSpace;
import org.emdev.utils.textmarkup.MarkupNote;
import org.emdev.utils.textmarkup.MarkupParagraphEnd;
import org.emdev.utils.textmarkup.MarkupTable;
import org.emdev.utils.textmarkup.MarkupTable.Cell;
import org.emdev.utils.textmarkup.MarkupTitle;
import org.emdev.utils.textmarkup.Words;
import org.emdev.utils.textmarkup.JustificationMode;
import org.emdev.utils.textmarkup.MarkupElement;
import org.emdev.utils.textmarkup.RenderingStyle;
import org.emdev.utils.textmarkup.RenderingStyle.Script;
import org.emdev.utils.textmarkup.line.TextElement;
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

    final SparseArray<Words> words = new SparseArray<Words>();

    int sectionLevel = -1;

    private boolean skipContent = true;

    public final ParsedContent parsedContent;
    String currentStream = null;
    String oldStream = null;

    private MarkupTable currentTable;

    public FB2ContentHandler(ParsedContent content) {
        parsedContent = content;
        currentStream = null;
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
            throws SAXException {
        spaceNeeded = true;
        final ArrayList<MarkupElement> markupStream = parsedContent.getMarkupStream(currentStream);
        if ("p".equals(qName)) {
            paragraphParsing = true;
            if (!parsingNotes) {
                if (!inTitle) {
                    markupStream.add(crs.paint.pOffset);
                }
            }
        } else if ("v".equals(qName)) {
            paragraphParsing = true;
            markupStream.add(crs.paint.pOffset);
            markupStream.add(crs.paint.vOffset);
        } else if ("binary".equals(qName)) {
            tmpBinaryName = attributes.getValue("id");
            tmpBinaryContents.setLength(0);
            parsingBinary = true;
        } else if ("body".equals(qName)) {
            if (!documentStarted && !documentEnded) {
                documentStarted = true;
                skipContent = false;
                currentStream = null;
            }
            if ("notes".equals(attributes.getValue("name"))) {
                if (documentStarted) {
                    documentEnded = true;
                    parsedContent.getMarkupStream(null).add(new MarkupEndDocument());
                }
                parsingNotes = true;
                crs = new RenderingStyle(FontStyle.FOOTNOTE);
            }
        } else if ("section".equals(qName)) {
            if (parsingNotes) {
                currentStream = attributes.getValue("id");
                if (currentStream != null) {
                    final String n = getNoteId(currentStream, true);
                    parsedContent.getMarkupStream(currentStream).add(text(n.toCharArray(), 0, n.length(), crs));
                    parsedContent.getMarkupStream(currentStream).add(crs.paint.fixedSpace);
                }
            } else {
                inSection = true;
                sectionLevel++;
            }
        } else if ("title".equals(qName)) {
            if (!parsingNotes) {
                setTitleStyle(!inSection ? FontStyle.MAIN_TITLE : FontStyle.SECTION_TITLE);
                markupStream.add(crs.jm);
                markupStream.add(emptyLine(crs.textSize));
                markupStream.add(MarkupParagraphEnd.E);
                title.setLength(0);
            } else {
                skipContent = true;
            }
            inTitle = true;
        } else if ("cite".equals(qName)) {
            inCite = true;
            setEmphasisStyle();
            markupStream.add(emptyLine(crs.textSize));
            markupStream.add(MarkupParagraphEnd.E);
        } else if ("subtitle".equals(qName)) {
            paragraphParsing = true;
            markupStream.add(setSubtitleStyle().jm);
            markupStream.add(emptyLine(crs.textSize));
            markupStream.add(MarkupParagraphEnd.E);
        } else if ("text-author".equals(qName) || ("date".equals(qName) && (documentStarted && !documentEnded || parsingNotes))) {
            paragraphParsing = true;
            markupStream.add(setTextAuthorStyle(inCite).jm);
            markupStream.add(crs.paint.pOffset);
        } else if ("a".equals(qName)) {
            if (paragraphParsing) {
                if ("note".equalsIgnoreCase(attributes.getValue("type"))) {
                    String note = attributes.getValue("href");
                    markupStream.add(new MarkupNote(note));
                    String prettyNote = " " + getNoteId(note, false);
                    markupStream.add(MarkupNoSpace._instance);
                    markupStream.add(
                            new TextElement(prettyNote.toCharArray(), 0, prettyNote.length(), new RenderingStyle(
                                    crs, Script.SUPER)));
                    skipContent = true;
                }
            }
        } else if ("empty-line".equals(qName)) {
            markupStream.add(emptyLine(crs.textSize));
            markupStream.add(MarkupParagraphEnd.E);
        } else if ("poem".equals(qName)) {
            markupStream.add(MarkupParagraphEnd.E);
            markupStream.add(emptyLine(crs.textSize));
            markupStream.add(setPoemStyle().jm);
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
            markupStream.add(MarkupParagraphEnd.E);
            markupStream.add(setEpigraphStyle().jm);
        } else if ("image".equals(qName)) {
            final String ref = attributes.getValue("href");
            if (cover) {
                parsedContent.setCover(ref);
            } else {
                if (!paragraphParsing) {
                    markupStream.add(emptyLine(crs.textSize));
                    markupStream.add(MarkupParagraphEnd.E);
                }
                markupStream.add(new MarkupImageRef(ref, paragraphParsing));
                if (!paragraphParsing) {
                    markupStream.add(emptyLine(crs.textSize));
                    markupStream.add(MarkupParagraphEnd.E);
                }
            }
        } else if ("coverpage".equals(qName)) {
            cover = true;
        } else if ("annotation".equals(qName)) {
            skipContent = false;
        } else if ("table".equals(qName)) {
            currentTable = new MarkupTable();
            markupStream.add(currentTable);
        } else if ("tr".equals(qName)) {
            if (currentTable != null) {
                currentTable.addRow();
            }
        } else if ("td".equals(qName) || "th".equals(qName)) {
            if (currentTable != null) {
                final int rowCount = currentTable.getRowCount();
                final Cell c = currentTable.new Cell();
                currentTable.addCol(c);
                final String streamId = currentTable.uuid + ":" + rowCount + ":" + currentTable.getColCount(rowCount - 1);
                c.stream = streamId;
                c.hasBackground = "th".equals(qName);
                final String align = attributes.getValue("align");
                if ("right".equals(align)) {
                    c.align = JustificationMode.Right;
                }
                if ("center".equals(align)) {
                    c.align = JustificationMode.Center;
                }
                paragraphParsing = true;
                oldStream = currentStream;
                currentStream = streamId;
            }
        }
    }

    private MarkupElement emptyLine(final int textSize) {
        return crs.paint.emptyLine;
    }

    private TextElement text(final char[] ch, final int st, final int len, final RenderingStyle style) {
        Words w = words.get(style.paint.key);
        if (w == null) {
            w = new Words();
            words.append(style.paint.key, w);
        }
        return w.get(ch, st, len, style);
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        spaceNeeded = true;
        final ArrayList<MarkupElement> markupStream = parsedContent.getMarkupStream(currentStream);
        if ("p".equals(qName) || "v".equals(qName)) {
            if (!skipContent) {
                markupStream.add(MarkupParagraphEnd.E);
            }
            paragraphParsing = false;
        } else if ("binary".equals(qName)) {
            if (tmpBinaryContents.length() > 0) {
                parsedContent.addImage(tmpBinaryName, tmpBinaryContents.toString());
                tmpBinaryName = null;
                tmpBinaryContents.setLength(0);
            }
            parsingBinary = false;
        } else if ("body".equals(qName)) {
            parsingNotes = false;
            currentStream = null;
        } else if ("section".equals(qName)) {
            if (parsingNotes) {
                noteId = -1;
                noteFirstWord = true;
            } else {
                if (inSection) {
                    markupStream.add(MarkupEndPage.E);
                    sectionLevel--;
                    inSection = false;
                }
            }
        } else if ("title".equals(qName)) {
            inTitle = false;
            skipContent = false;
            if (!parsingNotes) {
                markupStream.add(new MarkupTitle(title.toString(), sectionLevel));
                markupStream.add(emptyLine(crs.textSize));
                markupStream.add(MarkupParagraphEnd.E);
                markupStream.add(setPrevStyle().jm);
            }
        } else if ("cite".equals(qName)) {
            inCite = false;
            markupStream.add(emptyLine(crs.textSize));
            markupStream.add(MarkupParagraphEnd.E);
            markupStream.add(setPrevStyle().jm);
        } else if ("subtitle".equals(qName)) {
            markupStream.add(MarkupParagraphEnd.E);
            markupStream.add(emptyLine(crs.textSize));
            markupStream.add(MarkupParagraphEnd.E);
            markupStream.add(setPrevStyle().jm);
            paragraphParsing = false;
        } else if ("text-author".equals(qName) || "date".equals(qName)) {
            markupStream.add(MarkupParagraphEnd.E);
            markupStream.add(setPrevStyle().jm);
            paragraphParsing = false;
        } else if ("stanza".equals(qName)) {
            markupStream.add(emptyLine(crs.textSize));
            markupStream.add(MarkupParagraphEnd.E);
        } else if ("poem".equals(qName)) {
            markupStream.add(emptyLine(crs.textSize));
            markupStream.add(MarkupParagraphEnd.E);
            markupStream.add(setPrevStyle().jm);
        } else if ("strong".equals(qName)) {
            setPrevStyle();
            spaceNeeded = false;
        } else if ("strikethrough".equals(qName)) {
            setPrevStyle();
        } else if ("sup".equals(qName)) {
            setPrevStyle();
            if (markupStream.get(markupStream.size() - 1) instanceof MarkupNoSpace) {
                markupStream.remove(markupStream.size() - 1);
            }
        } else if ("sub".equals(qName)) {
            setPrevStyle();
            if (markupStream.get(markupStream.size() - 1) instanceof MarkupNoSpace) {
                markupStream.remove(markupStream.size() - 1);
            }
        } else if ("emphasis".equals(qName)) {
            setPrevStyle();
            spaceNeeded = false;
        } else if ("epigraph".equals(qName)) {
            markupStream.add(setPrevStyle().jm);
        } else if ("coverpage".equals(qName)) {
            cover = false;
        } else if ("a".equals(qName)) {
            if (paragraphParsing) {
                skipContent = false;
            }
        } else if ("annotation".equals(qName)) {
            skipContent = true;
            parsedContent.getMarkupStream(null).add(MarkupEndPage.E);
        } else if ("table".equals(qName)) {
            currentTable = null;
        } else if ("td".equals(qName) || "th".equals(qName)) {
            paragraphParsing = false;
            currentStream = oldStream;
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
                final ArrayList<MarkupElement> markupStream = parsedContent.getMarkupStream(currentStream);
                if (!spaceNeeded && !Character.isWhitespace(ch[start])) {
                    markupStream.add(MarkupNoSpace._instance);
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
                    markupStream.add(text(ch, st, len, crs));
                    if (crs.script != null) {
                        markupStream.add(MarkupNoSpace._instance);
                    }
                }
                if (Character.isWhitespace(ch[start + length - 1])) {
                    markupStream.add(MarkupNoSpace._instance);
                    markupStream.add(crs.paint.space);
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
