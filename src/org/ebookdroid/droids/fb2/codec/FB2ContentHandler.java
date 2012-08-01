package org.ebookdroid.droids.fb2.codec;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.emdev.common.lang.StrBuilder;
import org.emdev.common.textmarkup.JustificationMode;
import org.emdev.common.textmarkup.MarkupElement;
import org.emdev.common.textmarkup.MarkupEndDocument;
import org.emdev.common.textmarkup.MarkupEndPage;
import org.emdev.common.textmarkup.MarkupImageRef;
import org.emdev.common.textmarkup.MarkupNoSpace;
import org.emdev.common.textmarkup.MarkupNote;
import org.emdev.common.textmarkup.MarkupParagraphEnd;
import org.emdev.common.textmarkup.MarkupTable;
import org.emdev.common.textmarkup.MarkupTable.Cell;
import org.emdev.common.textmarkup.MarkupTitle;
import org.emdev.common.textmarkup.RenderingStyle;
import org.emdev.common.textmarkup.RenderingStyle.Script;
import org.emdev.common.textmarkup.TextStyle;
import org.emdev.common.textmarkup.Words;
import org.emdev.common.textmarkup.line.TextElement;
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

    private final StrBuilder tmpTagContent = new StrBuilder(16 * 1024);

    final SparseArray<Words> words = new SparseArray<Words>();

    int sectionLevel = -1;

    private boolean skipContent = true;

    private MarkupTable currentTable;

    private byte[] tagStack = new byte[10];
    private int tagStackSize = 0;

    public FB2ContentHandler(ParsedContent content) {
        super(content);
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
            throws SAXException {
        spaceNeeded = true;
        final ArrayList<MarkupElement> markupStream = parsedContent.getMarkupStream(currentStream);

        final byte tag = FB2Tag.getTagByName(qName);
        byte[] tmpTagStack = tagStack;
        if (tmpTagStack.length == tagStackSize) {
            byte[] newTagStack = new byte[tagStackSize * 2];
            if (tagStackSize > 0) {
                System.arraycopy(tmpTagStack, 0, newTagStack, 0, tagStackSize);
            }
            tmpTagStack = newTagStack;
            tagStack = tmpTagStack;
        }
        tmpTagStack[tagStackSize++] = tag;

        switch (tag) {
            case FB2Tag.P:
                paragraphParsing = true;
                if (!parsingNotes) {
                    if (!inTitle) {
                        markupStream.add(crs.paint.pOffset);
                    }
                }
                break;
            case FB2Tag.V:
                paragraphParsing = true;
                markupStream.add(crs.paint.pOffset);
                markupStream.add(crs.paint.vOffset);
                break;
            case FB2Tag.BINARY:
                tmpBinaryName = attributes.getValue("id");
                tmpBinaryContents.setLength(0);
                parsingBinary = true;
                break;
            case FB2Tag.BODY:
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
                    crs = new RenderingStyle(parsedContent, TextStyle.FOOTNOTE);
                }
                break;
            case FB2Tag.SECTION:
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
                break;
            case FB2Tag.TITLE:
                if (!parsingNotes) {
                    setTitleStyle(!inSection ? TextStyle.MAIN_TITLE : TextStyle.SECTION_TITLE);
                    markupStream.add(crs.jm);
                    markupStream.add(emptyLine(crs.textSize));
                    markupStream.add(MarkupParagraphEnd.E);
                    title.setLength(0);
                } else {
                    skipContent = true;
                }
                inTitle = true;
                break;
            case FB2Tag.CITE:
                inCite = true;
                setEmphasisStyle();
                markupStream.add(emptyLine(crs.textSize));
                markupStream.add(MarkupParagraphEnd.E);
                break;
            case FB2Tag.SUBTITLE:
                paragraphParsing = true;
                markupStream.add(setSubtitleStyle().jm);
                markupStream.add(emptyLine(crs.textSize));
                markupStream.add(MarkupParagraphEnd.E);
                break;
            case FB2Tag.TEXT_AUTHOR:
                paragraphParsing = true;
                markupStream.add(setTextAuthorStyle(inCite).jm);
                markupStream.add(crs.paint.pOffset);
                break;
            case FB2Tag.DATE:
                if (documentStarted && !documentEnded || parsingNotes) {
                    paragraphParsing = true;
                    markupStream.add(setTextAuthorStyle(inCite).jm);
                    markupStream.add(crs.paint.pOffset);
                }
                break;
            case FB2Tag.A:
                if (paragraphParsing) {
                    if ("note".equalsIgnoreCase(attributes.getValue("type"))) {
                        String note = attributes.getValue("href");
                        markupStream.add(new MarkupNote(note));
                        String prettyNote = " " + getNoteId(note, false);
                        markupStream.add(MarkupNoSpace._instance);
                        markupStream.add(new TextElement(prettyNote.toCharArray(), 0, prettyNote.length(),
                                new RenderingStyle(crs, Script.SUPER)));
                        skipContent = true;
                    }
                }
                break;
            case FB2Tag.EMPTY_LINE:
                markupStream.add(emptyLine(crs.textSize));
                markupStream.add(MarkupParagraphEnd.E);
                break;
            case FB2Tag.POEM:
                markupStream.add(MarkupParagraphEnd.E);
                markupStream.add(emptyLine(crs.textSize));
                markupStream.add(setPoemStyle().jm);
                break;
            case FB2Tag.STRONG:
                setBoldStyle();
                break;
            case FB2Tag.SUP:
                setSupStyle();
                spaceNeeded = false;
                break;
            case FB2Tag.SUB:
                setSubStyle();
                spaceNeeded = false;
                break;
            case FB2Tag.STRIKETHROUGH:
                setStrikeThrough();
                break;
            case FB2Tag.EMPHASIS:
                setEmphasisStyle();
                break;
            case FB2Tag.EPIGRAPH:
                markupStream.add(MarkupParagraphEnd.E);
                markupStream.add(setEpigraphStyle().jm);
                break;
            case FB2Tag.IMAGE:
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
                break;
            case FB2Tag.COVERPAGE:
                cover = true;
                break;
            case FB2Tag.ANNOTATION:
                skipContent = false;
                break;
            case FB2Tag.TABLE:
                currentTable = new MarkupTable();
                markupStream.add(currentTable);
                break;
            case FB2Tag.TR:
                if (currentTable != null) {
                    currentTable.addRow();
                }
                break;
            case FB2Tag.TD:
            case FB2Tag.TH:
                if (currentTable != null) {
                    final int rowCount = currentTable.getRowCount();
                    final Cell c = currentTable.new Cell();
                    currentTable.addCol(c);
                    final String streamId = currentTable.uuid + ":" + rowCount + ":"
                            + currentTable.getColCount(rowCount - 1);
                    c.stream = streamId;
                    c.hasBackground = tag == FB2Tag.TH;
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
                break;

        }
        tmpTagContent.setLength(0);
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
        if (tmpTagContent.length() > 0) {
            processTagContent();
        }
        spaceNeeded = true;
        final ArrayList<MarkupElement> markupStream = parsedContent.getMarkupStream(currentStream);
        final byte tag = tagStack[--tagStackSize];
        switch (tag) {
            case FB2Tag.P:
            case FB2Tag.V:
                if (!skipContent) {
                    markupStream.add(MarkupParagraphEnd.E);
                }
                paragraphParsing = false;
                break;
            case FB2Tag.BINARY:
                if (tmpBinaryContents.length() > 0) {
                    parsedContent.addImage(tmpBinaryName, tmpBinaryContents.toString());
                    tmpBinaryName = null;
                    tmpBinaryContents.setLength(0);
                }
                parsingBinary = false;
                break;
            case FB2Tag.BODY:
                parsingNotes = false;
                currentStream = null;
                break;
            case FB2Tag.SECTION:
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
                break;
            case FB2Tag.TITLE:
                inTitle = false;
                skipContent = false;
                if (!parsingNotes) {
                    markupStream.add(new MarkupTitle(title.toString(), sectionLevel));
                    markupStream.add(emptyLine(crs.textSize));
                    markupStream.add(MarkupParagraphEnd.E);
                    markupStream.add(setPrevStyle().jm);
                }
                break;
            case FB2Tag.CITE:
                inCite = false;
                markupStream.add(emptyLine(crs.textSize));
                markupStream.add(MarkupParagraphEnd.E);
                markupStream.add(setPrevStyle().jm);
                break;
            case FB2Tag.SUBTITLE:
                markupStream.add(MarkupParagraphEnd.E);
                markupStream.add(emptyLine(crs.textSize));
                markupStream.add(MarkupParagraphEnd.E);
                markupStream.add(setPrevStyle().jm);
                paragraphParsing = false;
                break;
            case FB2Tag.TEXT_AUTHOR:
            case FB2Tag.DATE:
                markupStream.add(MarkupParagraphEnd.E);
                markupStream.add(setPrevStyle().jm);
                paragraphParsing = false;
                break;
            case FB2Tag.STANZA:
                markupStream.add(emptyLine(crs.textSize));
                markupStream.add(MarkupParagraphEnd.E);
                break;
            case FB2Tag.POEM:
                markupStream.add(emptyLine(crs.textSize));
                markupStream.add(MarkupParagraphEnd.E);
                markupStream.add(setPrevStyle().jm);
                break;
            case FB2Tag.STRONG:
                setPrevStyle();
                spaceNeeded = false;
                break;
            case FB2Tag.STRIKETHROUGH:
                setPrevStyle();
                break;
            case FB2Tag.SUP:
                setPrevStyle();
                if (markupStream.get(markupStream.size() - 1) instanceof MarkupNoSpace) {
                    markupStream.remove(markupStream.size() - 1);
                }
                break;
            case FB2Tag.SUB:
                setPrevStyle();
                if (markupStream.get(markupStream.size() - 1) instanceof MarkupNoSpace) {
                    markupStream.remove(markupStream.size() - 1);
                }
                break;
            case FB2Tag.EMPHASIS:
                setPrevStyle();
                spaceNeeded = false;
                break;
            case FB2Tag.EPIGRAPH:
                markupStream.add(setPrevStyle().jm);
                break;
            case FB2Tag.COVERPAGE:
                cover = false;
                break;
            case FB2Tag.A:
                if (paragraphParsing) {
                    skipContent = false;
                }
                break;
            case FB2Tag.ANNOTATION:
                skipContent = true;
                parsedContent.getMarkupStream(null).add(MarkupEndPage.E);
                break;
            case FB2Tag.TABLE:
                currentTable = null;
                break;
            case FB2Tag.TD:
            case FB2Tag.TH:
                paragraphParsing = false;
                currentStream = oldStream;
                break;
        }
    }

    private void processTagContent() {
        final int length = tmpTagContent.length();
        final int start = 0;
        char[] ch = tmpTagContent.getValue();
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
        tmpTagContent.setLength(0);
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
            tmpTagContent.append(ch, start, length);
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
