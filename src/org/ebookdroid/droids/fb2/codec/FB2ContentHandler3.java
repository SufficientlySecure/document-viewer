package org.ebookdroid.droids.fb2.codec;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.xml.sax.SAXException;

public class FB2ContentHandler3 extends FB2BaseHandler {

    public enum ParserState {
        WAITING_FOR_TAG_START, WAITING_FOR_PREAMBLE
    }


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

    private class XmlReader {

        public final char[] XmlDoc;
        public int XmlOffset = 0;
        public final int XmlLength;
        private int[] stack = new int[1024];
        private int stackOffset = 0;

        public XmlReader(char[] xmlDoc, int xmlLength) {
            XmlDoc = xmlDoc;
            XmlLength = xmlLength;
        }

        public boolean skipChar(char c) {
            if (XmlDoc[XmlOffset] == c) {
                XmlOffset++;
                return true;
            }
            return false;
        }

        public void push() {
            stack[stackOffset++] = XmlOffset;
        }

        public void pop() {
            XmlOffset = stack[--stackOffset];
        }

        public void skipComment() {
            while (XmlOffset < XmlLength) {
                push();
                if (skipChar('-') && skipChar('-') && skipChar('>')) {
                    break;
                }
                pop();
                XmlOffset++;
            }
        }

        public void skipTagName() {
            while (XmlOffset < XmlLength) {
                if (Character.isWhitespace(XmlDoc[XmlOffset])
                        || (XmlDoc[XmlOffset] == '/' && XmlDoc[XmlOffset + 1] == '>') || XmlDoc[XmlOffset] == '>') {
                    break;
                }
                XmlOffset++;
            }
        }

        public void skipTo(char c) {
            while (XmlOffset < XmlLength) {
                if (XmlDoc[XmlOffset] == c) {
                    break;
                }
                XmlOffset++;
            }
        }

        public void skipToEndTag() {
            while (XmlOffset < XmlLength) {
                if ((XmlDoc[XmlOffset] == '/' && XmlDoc[XmlOffset + 1] == '>') || XmlDoc[XmlOffset] == '>') {
                    break;
                }
                XmlOffset++;
            }
        }

        public String[] fillAttributes(FB2Tag tag) {
            if (tag.attributes.length == 0) {
                return null;
            }
            String[] res = new String[tag.attributes.length];
            push();
            int start = XmlOffset;
            skipToEndTag();
            int end = XmlOffset;
            pop();
            String attrs = new String(XmlDoc, start, end - start);
            final String[] pairs = attrs.split(" ");
            for (String pair : pairs) {
                final String[] split = pair.split("=");
                if (split.length == 2) {
                    String attrName = split[0];
                    String attrValue = split[1];
                    final String[] split2 = attrName.split(":");
                    attrName = split2[split2.length - 1];
                    if (attrValue.startsWith("\"") && attrValue.endsWith("\"")) {
                        attrValue = attrValue.substring(1, attrValue.length() - 1);
                    }
                    final int i = Arrays.binarySearch(tag.attributes, attrName);
                    if (i >= 0) {
                        res[i] = attrValue;
                    }
                }
            }
            return res;
        }

    }

    public FB2ContentHandler3(ParsedContent content) {
        super(content);
    }

    public void parse(char[] xmlChars, int length) throws Exception {
        XmlReader r = new XmlReader(xmlChars, length);

        int charsStart = -1;
        boolean entityPresent = false;
        while (r.XmlOffset < length) {
            if (r.skipChar('<')) {
                if (charsStart != -1) {
                    charactersEntytyResolve(r.XmlDoc, charsStart, r.XmlOffset - 1 - charsStart, entityPresent);
                    charsStart = -1;
                    entityPresent = false;
                }
                r.push();
                if (r.skipChar('!') && r.skipChar('-') && r.skipChar('-')) {
                    r.pop();
                    r.skipComment();
                    continue;
                } else {
                    r.pop();
                }
                if (r.skipChar('/')) {
                    int tagNameStart = r.XmlOffset;
                    r.skipTagName();
                    String tagName = new String(r.XmlDoc, tagNameStart, r.XmlOffset - tagNameStart);
                    FB2Tag tag = FB2Tag.getTagByName(tagName);
                    endElement(tag);
                    r.skipTo('>');
                    r.XmlOffset++;
                    continue;
                } else {
                    int tagNameStart = r.XmlOffset;
                    r.skipTagName();
                    String tagName = new String(r.XmlDoc, tagNameStart, r.XmlOffset - tagNameStart);
                    FB2Tag tag = FB2Tag.getTagByName(tagName);
                    if (tag.attributes.length == 0) {
                        startElement(tag);
                    } else {
                        String[] attributes = r.fillAttributes(tag);
                        startElement(tag, attributes);
                    }
                    r.skipToEndTag();

                    if (r.skipChar('/') && r.skipChar('>')) {
                        endElement(tag);
                        continue;
                    } else {
                        r.XmlOffset++;
                        continue;
                    }
                }
            } else {
                if (charsStart == -1) {
                    charsStart = r.XmlOffset;
                }
                if (r.XmlDoc[r.XmlOffset] == '&') {
                    entityPresent = true;
                }
                r.XmlOffset++;
                continue;
            }
        }
    }

    private void charactersEntytyResolve(char[] xmlDoc, int start, int len, boolean entityPresent) {
        if (!entityPresent) {
            characters(xmlDoc, start, len);
            return;
        }
        int st = start;
        int i = start;

        while (i <= start + len) {
            if (xmlDoc[i] == '&' || i == start + len) {
                characters(xmlDoc, st, i - st);
                int j = i + 1;
                while (j < start + len) {
                    if (xmlDoc[j] == ';') {
                        char[] ch = new char[1];
                        if (xmlDoc[i + 1] == '#') {
                            // numeric
                            if (xmlDoc[i + 2] == 'x' || xmlDoc[i + 2] == 'X') {
                                ch[0] = (char) Integer.parseInt(new String(xmlDoc, i + 3, j - i - 3), 16);
                            } else {
                                ch[0] = (char) Integer.parseInt(new String(xmlDoc, i + 2, j - i - 2));
                            }
                        } else {
                            String e = new String(xmlDoc, i + 1, j - i - 1);
                            if ("qout".equals(e)) {
                                ch[0] = 34;
                            } else if ("amp".equals(e)) {
                                ch[0] = 38;
                            } else if ("apos".equals(e)) {
                                ch[0] = 39;
                            } else if ("lt".equals(e)) {
                                ch[0] = 60;
                            } else if ("gt".equals(e)) {
                                ch[0] = 62;
                            }
                        }
                        characters(ch, 0, 1);
                        // entity end
                        i = j;
                        break;
                    }
                    j++;
                }
                st = i + 1;
            }
            i++;
        }
    }

    public void startElement(final FB2Tag tag, final String... attributes) throws SAXException {
        spaceNeeded = true;
        final ArrayList<MarkupElement> markupStream = parsedContent.getMarkupStream(currentStream);

        switch (tag.tag) {
            case FB2Tag.P:
                paragraphParsing = true;
                if (!parsingNotes) {
                    if (!inTitle) {
                        markupStream.add(crs.paint.pOffset);
                    } else {
                        if (title.length() > 0) {
                            title.append(" ");
                        }
                    }
                }
                break;
            case FB2Tag.V:
                paragraphParsing = true;
                markupStream.add(crs.paint.pOffset);
                markupStream.add(crs.paint.vOffset);
                break;
            case FB2Tag.BINARY:
                tmpBinaryName = attributes[0];
                tmpBinaryContents.setLength(0);
                parsingBinary = true;
                break;
            case FB2Tag.BODY:
                if (!documentStarted && !documentEnded) {
                    documentStarted = true;
                    skipContent = false;
                    currentStream = null;
                }
                if ("notes".equals(attributes[0])) {
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
                    currentStream = attributes[0];
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
                    if ("note".equalsIgnoreCase(attributes[1])) {
                        String note = attributes[0];
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
                final String ref = attributes[0];
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
                    c.hasBackground = tag.tag == FB2Tag.TH;
                    final String align = attributes[0];
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
        return new TextElement(ch, st, len, style);
    }

    public void endElement(final FB2Tag tag) {
        spaceNeeded = true;
        final ArrayList<MarkupElement> markupStream = parsedContent.getMarkupStream(currentStream);
        switch (tag.tag) {
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

    public void characters(final char[] ch, final int start, final int length) {
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
