package org.ebookdroid.droids.fb2.codec.handlers;

import org.ebookdroid.droids.fb2.codec.FB2Page;
import org.ebookdroid.droids.fb2.codec.ParsedContent;
import org.ebookdroid.droids.fb2.codec.tags.FB2Tag;
import org.ebookdroid.droids.fb2.codec.tags.FB2TagId;

import java.util.ArrayList;

import org.emdev.common.fonts.data.FontStyle;
import org.emdev.common.lang.StrBuilder;
import org.emdev.common.textmarkup.JustificationMode;
import org.emdev.common.textmarkup.MarkupElement;
import org.emdev.common.textmarkup.MarkupEndDocument;
import org.emdev.common.textmarkup.MarkupEndPage;
import org.emdev.common.textmarkup.MarkupExtraSpace;
import org.emdev.common.textmarkup.MarkupImageRef;
import org.emdev.common.textmarkup.MarkupNoLineBreak;
import org.emdev.common.textmarkup.MarkupNoSpace;
import org.emdev.common.textmarkup.MarkupNote;
import org.emdev.common.textmarkup.MarkupParagraphEnd;
import org.emdev.common.textmarkup.MarkupTable;
import org.emdev.common.textmarkup.MarkupTable.Cell;
import org.emdev.common.textmarkup.MarkupTitle;
import org.emdev.common.textmarkup.RenderingStyle;
import org.emdev.common.textmarkup.RenderingStyle.Script;
import org.emdev.common.textmarkup.TextStyle;
import org.emdev.common.textmarkup.line.LineFixedWhiteSpace;
import org.emdev.common.textmarkup.line.LineWhiteSpace;
import org.emdev.common.textmarkup.line.TextElement;
import org.emdev.common.textmarkup.line.TextPreElement;
import org.emdev.common.xml.IContentHandler;
import org.emdev.common.xml.TextProvider;
import org.emdev.common.xml.tags.XmlTag;
import org.emdev.utils.StringUtils;

public class StandardHandler extends BaseHandler implements IContentHandler, FB2TagId {

    protected boolean documentStarted = false, documentEnded = false;

    protected boolean paragraphParsing = false;

    protected int ulLevel = 0;

    protected boolean cover = false;

    protected String tmpBinaryName = null;
    protected boolean parsingNotes = false;
    protected boolean parsingBinary = false;
    protected boolean inTitle = false;
    protected boolean inEpigraph = false;
    protected boolean inCite = false;
    protected boolean noteFirstWord = true;
    protected boolean parseNotesInParagraphs = false;

    protected boolean spaceNeeded = true;

    protected StrBuilder tmpBinaryContent = null;
    protected char[] tmpBinary = null;
    protected int tmpBinaryStart = 0;
    protected int tmpBinaryLength = 0;

    protected final StringBuilder title = new StringBuilder();

    protected final StrBuilder tmpTagContent = new StrBuilder(16 * 1024);

    protected int sectionLevel = -1;

    protected boolean skipContent = true;

    protected MarkupTable currentTable;

    private boolean parsingPreformatted = false;
    private int parsingPreformattedLevel = -1;
    private int parsingPreformattedLines = 0;
    protected int tagLevel = 0;

    public StandardHandler(final ParsedContent content) {
        super(content);
    }

    @Override
    public boolean parseAttributes(final XmlTag tag) {
        if (tag == FB2Tag.P.tag) {
            return parseNotesInParagraphs && tag.attributes.length > 0;
        }
        return tag.attributes.length > 0;
    }

    @Override
    public void startElement(final XmlTag tag, final String... attributes) {
        tagLevel++;
        final ArrayList<MarkupElement> markupStream = parsedContent.getMarkupStream(currentStream);

        if (tmpTagContent.length() > 0) {
            processTagContent();
        }

        switch (tag.tag) {
            case P:
                paragraphParsing = true;
                if (!parsingNotes) {
                    if (!inTitle) {
                        markupStream.add(crs.paint.pOffset);
                    } else {
                        if (title.length() > 0) {
                            title.append(" ");
                        }
                    }
                } else if (parseNotesInParagraphs) {
                    currentStream = attributes[0];
                    if (currentStream != null) {
                        final String n = getNoteId(currentStream, true);
                        parsedContent.getMarkupStream(currentStream).add(text(new TextProvider(n), 0, n.length(), crs));
                        parsedContent.getMarkupStream(currentStream).add(crs.paint.fixedSpace);
                        noteFirstWord = true;
                    }
                }
                break;
            case UL:
                markupStream.add(emptyLine(crs.textSize));
                markupStream.add(MarkupParagraphEnd.E);
                ulLevel++;
                break;
            case LI:
                paragraphParsing = true;
                markupStream.add(new MarkupExtraSpace((int) (crs.paint.pOffset.width * ulLevel)));
                markupStream.add(crs.bullet);
                markupStream.add(MarkupNoSpace.E);
                break;
            case V:
                paragraphParsing = true;
                markupStream.add(new MarkupExtraSpace((int) (crs.paint.pOffset.width + crs.paint.vOffset.width)));
                break;
            case BINARY:
                tmpBinaryName = attributes[0];
                tmpBinary = null;
                tmpBinaryContent = null;
                parsingBinary = true;
                break;
            case BODY:
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
                if ("footnotes".equals(attributes[0])) {
                    if (documentStarted) {
                        documentEnded = true;
                        parsedContent.getMarkupStream(null).add(new MarkupEndDocument());
                    }
                    parsingNotes = true;
                    parseNotesInParagraphs = true;
                    crs = new RenderingStyle(parsedContent, TextStyle.FOOTNOTE);
                }
                break;
            case SECTION:
                if (parsingNotes) {
                    if (!parseNotesInParagraphs) {
                        currentStream = attributes[0];
                        if (currentStream != null) {
                            final String n = getNoteId(currentStream, true);
                            parsedContent.getMarkupStream(currentStream).add(
                                    text(new TextProvider(n), 0, n.length(), crs));
                            parsedContent.getMarkupStream(currentStream).add(crs.paint.fixedSpace);
                            noteFirstWord = true;
                        }
                    }
                } else {
                    sectionLevel++;
                }
                break;
            case TITLE:
                if (!parsingNotes) {
                    setTitleStyle(!isInSection() ? TextStyle.MAIN_TITLE : TextStyle.SECTION_TITLE);
                    markupStream.add(crs.jm);
                    markupStream.add(emptyLine(crs.textSize));
                    markupStream.add(MarkupParagraphEnd.E);
                    title.setLength(0);
                } else {
                    skipContent = true;
                }
                inTitle = true;
                break;
            case CITE:
                inCite = true;
                markupStream.add(emptyLine(crs.textSize));
                markupStream.add(MarkupParagraphEnd.E);
                markupStream.add(new MarkupExtraSpace(FB2Page.PAGE_WIDTH / 6));
                break;
            case SUBTITLE:
                paragraphParsing = true;
                markupStream.add(setSubtitleStyle().jm);
                markupStream.add(emptyLine(crs.textSize));
                markupStream.add(MarkupParagraphEnd.E);
                break;
            case TEXT_AUTHOR:
                paragraphParsing = true;
                markupStream.add(setTextAuthorStyle(inCite).jm);
                markupStream.add(crs.paint.pOffset);
                break;
            case DATE:
                if (documentStarted && !documentEnded || parsingNotes) {
                    paragraphParsing = true;
                    markupStream.add(setTextAuthorStyle(inCite).jm);
                    markupStream.add(crs.paint.pOffset);
                }
                break;
            case A:
                if (paragraphParsing) {
                    if ("note".equalsIgnoreCase(attributes[1])) {
                        if (markupStream.get(markupStream.size() - 1) instanceof LineWhiteSpace) {
                            markupStream.remove(markupStream.size() - 1);
                        }
                        final String note = attributes[0];
                        final String prettyNote = " " + getNoteId(note, false);
                        markupStream.add(MarkupNoSpace.E);
                        markupStream.add(MarkupNoLineBreak.E);
                        markupStream.add(new TextElement(prettyNote.toCharArray(), 0, prettyNote.length(),
                                new RenderingStyle(parsedContent, crs, Script.SUPER)));
                        markupStream.add(new MarkupNote(note));
                        skipContent = true;
                    }
                }
                break;
            case EMPTY_LINE:
            case BR:
                markupStream.add(emptyLine(crs.textSize));
                markupStream.add(MarkupParagraphEnd.E);
                break;
            case POEM:
                markupStream.add(MarkupParagraphEnd.E);
                markupStream.add(emptyLine(crs.textSize));
                markupStream.add(setPoemStyle().jm);
                break;
            case STRONG:
                setBoldStyle();
                break;
            case SUP:
                setSupStyle();
                spaceNeeded = false;
                break;
            case SUB:
                setSubStyle();
                spaceNeeded = false;
                break;
            case STRIKETHROUGH:
                setStrikeThrough();
                break;
            case EMPHASIS:
                setEmphasisStyle();
                break;
            case EPIGRAPH:
                inEpigraph = true;
                markupStream.add(MarkupParagraphEnd.E);
                markupStream.add(setEpigraphStyle().jm);
                break;
            case IMAGE:
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
            case COVERPAGE:
                cover = true;
                break;
            case ANNOTATION:
                skipContent = false;
                break;
            case TABLE:
                currentTable = new MarkupTable();
                markupStream.add(currentTable);
                break;
            case TR:
                if (currentTable != null) {
                    currentTable.addRow();
                }
                break;
            case TD:
            case TH:
                if (currentTable != null) {
                    paragraphParsing = true;

                    final Cell c = currentTable.addCol();
                    c.hasBackground = tag == FB2Tag.TH.tag;

                    final String align = attributes[0];
                    if ("right".equals(align)) {
                        c.align = JustificationMode.Right;
                    }
                    if ("center".equals(align)) {
                        c.align = JustificationMode.Center;
                    }

                    oldStream = currentStream;
                    currentStream = c.stream;
                }
                break;
            case CODE:
                parsingPreformatted = true;
                parsingPreformattedLevel = tagLevel;
                parsingPreformattedLines = 0;
                setPreformatted();

                if (markupStream.get(markupStream.size() - 1) instanceof LineFixedWhiteSpace) {
                    markupStream.remove(markupStream.size() - 1);
                }

            default:
                break;
        }
        tmpTagContent.setLength(0);
    }

    @Override
    public void endElement(final XmlTag tag) {
        tagLevel--;
        if (tmpTagContent.length() > 0) {
            processTagContent();
        }
        final ArrayList<MarkupElement> markupStream = parsedContent.getMarkupStream(currentStream);
        switch (tag.tag) {
            case LI:
                markupStream.add(new MarkupExtraSpace(-(int) (crs.paint.pOffset.width * ulLevel)));
            case P:
                if (!skipContent) {
                    if (crs.face.style != FontStyle.REGULAR && !inEpigraph) {
                        crs = new RenderingStyle(parsedContent, crs, FontStyle.REGULAR);
                    }
                    markupStream.add(MarkupParagraphEnd.E);
                }
                paragraphParsing = false;
                break;
            case V:
                markupStream.add(new MarkupExtraSpace(-(int) (crs.paint.pOffset.width + crs.paint.vOffset.width)));

                if (!skipContent) {
                    markupStream.add(MarkupParagraphEnd.E);
                }
                paragraphParsing = false;
                break;
            case UL:
                ulLevel--;
                markupStream.add(emptyLine(crs.textSize));
                markupStream.add(MarkupParagraphEnd.E);
                break;
            case BINARY:
                if (tmpBinary != null) {
                    parsedContent.addImage(tmpBinaryName, tmpBinary, tmpBinaryStart, tmpBinaryLength);
                    tmpBinaryName = null;
                    tmpBinary = null;
                } else if (tmpBinaryContent != null) {
                    parsedContent.addImage(tmpBinaryName, tmpBinaryContent.shareValue(), 0, tmpBinaryContent.length());
                    tmpBinaryName = null;
                    tmpBinaryContent = null;
                }
                parsingBinary = false;
                break;
            case BODY:
                parsingNotes = false;
                currentStream = null;
                parseNotesInParagraphs = false;
                break;
            case SECTION:
                if (parsingNotes) {
                    noteId = -1;
                } else {
                    if (isInSection()) {
                        markupStream.add(MarkupEndPage.E);
                        sectionLevel--;
                    }
                }
                break;
            case TITLE:
                inTitle = false;
                skipContent = false;
                if (!parsingNotes) {
                    markupStream.add(new MarkupTitle(title.toString(), sectionLevel));
                    markupStream.add(emptyLine(crs.textSize));
                    markupStream.add(MarkupParagraphEnd.E);
                    markupStream.add(setPrevStyle().jm);
                }
                break;
            case CITE:
                inCite = false;
                markupStream.add(emptyLine(crs.textSize));
                markupStream.add(MarkupParagraphEnd.E);
                markupStream.add(new MarkupExtraSpace(-FB2Page.PAGE_WIDTH / 6));
                break;
            case SUBTITLE:
                markupStream.add(MarkupParagraphEnd.E);
                markupStream.add(emptyLine(crs.textSize));
                markupStream.add(MarkupParagraphEnd.E);
                markupStream.add(setPrevStyle().jm);
                paragraphParsing = false;
                break;
            case TEXT_AUTHOR:
            case DATE:
                markupStream.add(MarkupParagraphEnd.E);
                markupStream.add(setPrevStyle().jm);
                paragraphParsing = false;
                break;
            case STANZA:
                markupStream.add(emptyLine(crs.textSize));
                markupStream.add(MarkupParagraphEnd.E);
                break;
            case POEM:
                markupStream.add(emptyLine(crs.textSize));
                markupStream.add(MarkupParagraphEnd.E);
                markupStream.add(setPrevStyle().jm);
                break;
            case STRONG:
                setPrevStyle();
                spaceNeeded = false;
                break;
            case STRIKETHROUGH:
                setPrevStyle();
                break;
            case SUP:
                setPrevStyle();
                if (markupStream.get(markupStream.size() - 1) instanceof MarkupNoSpace) {
                    markupStream.remove(markupStream.size() - 1);
                }
                break;
            case SUB:
                setPrevStyle();
                if (markupStream.get(markupStream.size() - 1) instanceof MarkupNoSpace) {
                    markupStream.remove(markupStream.size() - 1);
                }
                break;
            case EMPHASIS:
                setPrevStyle();
                spaceNeeded = false;
                break;
            case EPIGRAPH:
                markupStream.add(setPrevStyle().jm);
                inEpigraph = false;
                break;
            case COVERPAGE:
                cover = false;
                break;
            case A:
                if (paragraphParsing) {
                    skipContent = false;
                }
                break;
            case ANNOTATION:
                skipContent = true;
                parsedContent.getMarkupStream(null).add(MarkupEndPage.E);
                break;
            case TABLE:
                currentTable = null;
                break;
            case TD:
            case TH:
                paragraphParsing = false;
                currentStream = oldStream;
                break;
            case CODE:
                setPrevStyle();
                parsingPreformatted = false;
                parsingPreformattedLevel = -1;
                if (!paragraphParsing) {
                    markupStream.add(MarkupParagraphEnd.E);
                }
            default:
                break;
        }
    }

    protected boolean isInSection() {
        return sectionLevel >= 0;
    }

    @Override
    public boolean skipCharacters() {
        return skipContent
                || (!(documentStarted && !documentEnded) && !paragraphParsing && !parsingBinary && !parsingNotes);
    }

    @Override
    public void characters(final TextProvider text, final int start, final int length) {
        if (parsingBinary) {
            if (tmpBinary == null) {
                tmpBinary = text.chars;
                tmpBinaryStart = start;
                tmpBinaryLength = length;
            } else {
                tmpBinaryLength += length;
            }
        } else {
            processText(text, start, length);
        }
    }

    protected void processTagContent() {

        final int length = tmpTagContent.length();
        final int start = 0;
        final char[] ch = tmpTagContent.getValue();

        processText(new TextProvider(ch), start, length);

        tmpTagContent.setLength(0);
    }

    protected void processText(final TextProvider text, final int start, final int length) {
        if (inTitle) {
            title.append(text.chars, start, length);
        }
        if (!parsingPreformatted || tagLevel > parsingPreformattedLevel) {
            final int count = StringUtils.split(text.chars, start, length, starts, lengths, false);

            if (count > 0) {
                final ArrayList<MarkupElement> markupStream = parsedContent.getMarkupStream(currentStream);
                if (!spaceNeeded && !Character.isWhitespace(text.chars[start])) {
                    markupStream.add(MarkupNoSpace.E);
                }
                spaceNeeded = true;

                for (int i = 0; i < count; i++) {
                    final int st = starts[i];
                    final int len = lengths[i];
                    if (parsingNotes) {
                        if (noteFirstWord) {
                            noteFirstWord = false;
                            final int id = getNoteId(text.chars, st, len);
                            if (id == noteId) {
                                continue;
                            }
                        }
                    }
                    markupStream.add(text(text, st, len, crs));
                    if (crs.script != null) {
                        markupStream.add(MarkupNoSpace.E);
                    }
                }
                if (Character.isWhitespace(text.chars[start + length - 1])) {
                    markupStream.add(crs.paint.space);
                }
                spaceNeeded = false;
            }
        } else {
            final int count = StringUtils.split(text.chars, start, length, starts, lengths, true);
            if (count > 0) {
                final ArrayList<MarkupElement> markupStream = parsedContent.getMarkupStream(currentStream);
                for (int i = 0; i < count; i++) {
                    final int st = starts[i];
                    final int len = lengths[i];
                    if (!paragraphParsing || (parsingPreformattedLines++) > 0) {
                        markupStream.add(MarkupParagraphEnd.E);
                    }
                    markupStream.add(textPre(text, st, len, crs));
                }
            }
        }
    }

    protected TextElement text(final TextProvider text, final int st, final int len, final RenderingStyle style) {
        return new TextElement(text.chars, st, len, style);
    }

    protected TextElement textPre(final TextProvider text, final int st, final int len, final RenderingStyle style) {
        return new TextPreElement(text.chars, st, len, style);
    }

}
