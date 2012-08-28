package org.ebookdroid.droids.fb2.codec.handlers;

import org.ebookdroid.droids.fb2.codec.FB2Page;
import org.ebookdroid.droids.fb2.codec.FB2Tag;
import org.ebookdroid.droids.fb2.codec.ParsedContent;

import java.util.ArrayList;

import org.emdev.common.lang.StrBuilder;
import org.emdev.common.textmarkup.JustificationMode;
import org.emdev.common.textmarkup.MarkupElement;
import org.emdev.common.textmarkup.MarkupEndDocument;
import org.emdev.common.textmarkup.MarkupEndPage;
import org.emdev.common.textmarkup.MarkupExtraSpace;
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

public class StandardHandler extends BaseHandler implements IContentHandler {

    protected boolean documentStarted = false, documentEnded = false;

    protected boolean inSection = false;

    protected boolean paragraphParsing = false;
    
    protected int ulLevel = 0;

    protected boolean cover = false;

    protected String tmpBinaryName = null;
    protected boolean parsingNotes = false;
    protected boolean parsingBinary = false;
    protected boolean inTitle = false;
    protected boolean inCite = false;
    protected boolean noteFirstWord = true;

    protected boolean spaceNeeded = true;

    protected final StringBuilder tmpBinaryContents = new StringBuilder(64 * 1024);
    protected final StringBuilder title = new StringBuilder();

    protected final StrBuilder tmpTagContent = new StrBuilder(16 * 1024);

    protected int sectionLevel = -1;

    protected boolean skipContent = true;

    protected MarkupTable currentTable;

    protected boolean useUniqueTextElements;

    private static final char[] BULLET = "\u2022 ".toCharArray();

    public StandardHandler(final ParsedContent content) {
        this(content, true);
    }

    public StandardHandler(final ParsedContent content, final boolean useUniqueTextElements) {
        super(content);
        this.useUniqueTextElements = useUniqueTextElements;
    }

    @Override
    public void startElement(final FB2Tag tag, final String... attributes) {
        spaceNeeded = true;
        final ArrayList<MarkupElement> markupStream = parsedContent.getMarkupStream(currentStream);

        if (tmpTagContent.length() > 0) {
            processTagContent();
        }

        switch (tag) {
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
                markupStream.add(new TextElement(BULLET, 0, BULLET.length, crs));
                markupStream.add(MarkupNoSpace._instance);
                break;
            case V:
                paragraphParsing = true;
                markupStream.add(crs.paint.pOffset);
                markupStream.add(crs.paint.vOffset);
                break;
            case BINARY:
                tmpBinaryName = attributes[0];
                tmpBinaryContents.setLength(0);
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
                break;
            case SECTION:
                if (parsingNotes) {
                    currentStream = attributes[0];
                    if (currentStream != null) {
                        final String n = getNoteId(currentStream, true);
                        parsedContent.getMarkupStream(currentStream).add(
                                text(n.toCharArray(), 0, n.length(), crs, true));
                        parsedContent.getMarkupStream(currentStream).add(crs.paint.fixedSpace);
                    }
                } else {
                    inSection = true;
                    sectionLevel++;
                }
                break;
            case TITLE:
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
                        final String note = attributes[0];
                        markupStream.add(new MarkupNote(note));
                        final String prettyNote = " " + getNoteId(note, false);
                        markupStream.add(MarkupNoSpace._instance);
                        markupStream.add(new TextElement(prettyNote.toCharArray(), 0, prettyNote.length(),
                                new RenderingStyle(crs, Script.SUPER)));
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
                    final int rowCount = currentTable.getRowCount();
                    final Cell c = currentTable.new Cell();
                    currentTable.addCol(c);
                    final String streamId = currentTable.uuid + ":" + rowCount + ":"
                            + currentTable.getColCount(rowCount - 1);
                    c.stream = streamId;
                    c.hasBackground = tag == FB2Tag.TH;
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

    @Override
    public void endElement(final FB2Tag tag) {
        if (tmpTagContent.length() > 0) {
            processTagContent();
        }
        spaceNeeded = true;
        final ArrayList<MarkupElement> markupStream = parsedContent.getMarkupStream(currentStream);
        switch (tag) {
            case LI:
                markupStream.add(new MarkupExtraSpace(-(int) (crs.paint.pOffset.width * ulLevel)));
            case P:
            case V:
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
                if (tmpBinaryContents.length() > 0) {
                    parsedContent.addImage(tmpBinaryName, tmpBinaryContents.toString());
                    tmpBinaryName = null;
                    tmpBinaryContents.setLength(0);
                }
                parsingBinary = false;
                break;
            case BODY:
                parsingNotes = false;
                currentStream = null;
                break;
            case SECTION:
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
        }
    }

    @Override
    public boolean skipCharacters() {
        return skipContent
                || (!(documentStarted && !documentEnded) && !paragraphParsing && !parsingBinary && !parsingNotes);
    }

    @Override
    public void characters(final char[] ch, final int start, final int length, final boolean persistent) {
        if (parsingBinary) {
            tmpBinaryContents.append(ch, start, length);
        } else {
            if (persistent) {
                processText(ch, start, length, persistent);
            } else {
                tmpTagContent.append(ch, start, length);
            }
        }
    }

    protected void processTagContent() {

        final int length = tmpTagContent.length();
        final int start = 0;
        final char[] ch = tmpTagContent.getValue();

        processText(ch, start, length, false);

        tmpTagContent.setLength(0);
    }

    protected void processText(final char[] ch, final int start, final int length, final boolean persistent) {
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
                markupStream.add(text(ch, st, len, crs, persistent));
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

    protected TextElement text(final char[] ch, final int st, final int len, final RenderingStyle style,
            final boolean persistent) {
        if (!useUniqueTextElements && persistent) {
            return new TextElement(ch, st, len, style);
        }

        Words w = parsedContent.words.get(style.paint.key);
        if (w == null) {
            w = new Words();
            parsedContent.words.append(style.paint.key, w);
        }
        return w.get(ch, st, len, style, persistent);

    }

}
