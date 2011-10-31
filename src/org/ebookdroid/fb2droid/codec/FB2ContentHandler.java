package org.ebookdroid.fb2droid.codec;

import org.ebookdroid.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class FB2ContentHandler extends FB2BaseHandler {

    private boolean documentStarted = false, documentEnded = false;

    private boolean inSection = false;

    private boolean paragraphParsing = false;

    private boolean cover = false;

    private String tmpBinaryName = null;
    private boolean parsingNotes = false;
    private boolean parsingNotesP = false;
    private boolean parsingBinary = false;
    private boolean inTitle = false;
    private boolean inCite = false;
    private String noteName = null;
    private int noteId = -1;
    private boolean noteFirstWord = true;
    private ArrayList<FB2Line> noteLines = null;

    private boolean spaceNeeded = true;

    private static final Pattern notesPattern = Pattern.compile("n([0-9]+)|n_([0-9]+)");
    private final StringBuilder tmpBinaryContents = new StringBuilder(64 * 1024);
    private final StringBuilder title = new StringBuilder();

    final List<FB2MarkupElement> markup = new ArrayList<FB2MarkupElement>();

    public FB2ContentHandler(final FB2Document fb2Document) {
        super(fb2Document);
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
            throws SAXException {
        spaceNeeded = true;
        if ("p".equals(qName)) {
            if (parsingNotes && !inTitle) {
                parsingNotesP = true;
            } else {
                paragraphParsing = true;
                markup.add(new FB2MarkupNewParagraph(crs.textSize));
            }
        } else if ("v".equals(qName)) {
            if (parsingNotes && !inTitle) {
                parsingNotesP = true;
            } else {
                paragraphParsing = true;
                markup.add(new FB2MarkupNewParagraph(crs.textSize));
                markup.add(new FB2LineWhiteSpace(FB2Page.PAGE_WIDTH / 8, crs.textSize, false));
            }
        } else if ("binary".equals(qName)) {
            tmpBinaryName = attributes.getValue("id");
            tmpBinaryContents.setLength(0);
            parsingBinary = true;
        } else if ("body".equals(qName)) {
            if (!documentStarted && !documentEnded) {
                documentStarted = true;
            }
            if (documentEnded && "notes".equals(attributes.getValue("name"))) {
                parsingNotes = true;
                crs = new RenderingStyle(RenderingStyle.FOOTNOTE_SIZE);
            }
        } else if ("section".equals(qName)) {
            if (parsingNotes) {
                noteName = attributes.getValue("id");
                if (noteName != null) {
                    final String n = getNoteId();
                    noteLines = new ArrayList<FB2Line>();
                    FB2Line lastLine = new FB2Line();
                    noteLines.add(lastLine);
                    lastLine.append(new FB2HorizontalRule(FB2Page.PAGE_WIDTH / 4, RenderingStyle.FOOTNOTE_SIZE));
                    lastLine.applyJustification(JustificationMode.Left);
                    lastLine = new FB2Line();
                    noteLines.add(lastLine);
                    lastLine.append(new FB2TextElement(n.toCharArray(), 0, n.length(), crs)).append(
                            new FB2LineWhiteSpace((int) crs.getTextPaint().measureText(" "), crs.textSize, false));
                }
            } else {
                inSection = true;
            }
        } else if ("title".equals(qName)) {
            if (!parsingNotes) {
                setTitleStyle(!inSection ? RenderingStyle.MAIN_TITLE_SIZE : RenderingStyle.SECTION_TITLE_SIZE);
                markup.add(crs.jm);
                markup.add(emptyLine(crs.textSize));
                title.setLength(0);
            }
            inTitle = true;
        } else if ("cite".equals(qName)) {
            inCite = true;
            if (!parsingNotes) {
                setEmphasisStyle();
                markup.add(emptyLine(crs.textSize));
            }
        } else if ("subtitle".equals(qName)) {
            if (!parsingNotes) {
                paragraphParsing = true;
                markup.add(setSubtitleStyle().jm);
                markup.add(emptyLine(crs.textSize));
                markup.add(new FB2MarkupNewParagraph(crs.textSize));
            }
        } else if ("text-author".equals(qName)) {
            if (!parsingNotes) {
                paragraphParsing = true;
                markup.add(setTextAuthorStyle(inCite).jm);
                markup.add(new FB2MarkupNewParagraph(crs.textSize));
            }
        } else if ("a".equals(qName)) {
            if (paragraphParsing) {
                if ("note".equalsIgnoreCase(attributes.getValue("type"))) {
                    markup.add(new FB2MarkupNote(attributes.getValue("href")));
                }
            }
        } else if ("empty-line".equals(qName)) {
            markup.add(emptyLine(crs.textSize));
        } else if ("poem".equals(qName)) {
            if (!parsingNotes) {
                markup.add(setPoemStyle().jm);
                markup.add(emptyLine(crs.textSize));
            }
        } else if ("strong".equals(qName)) {
            setBoldStyle();
        } else if ("sup".equals(qName)) {
            setSupStyle();
            spaceNeeded = false;
        } else if ("sub".equals(qName)) {
            setSubStyle();
            spaceNeeded = false;
        } else if ("emphasis".equals(qName)) {
            setEmphasisStyle();
        } else if ("epigraph".equals(qName)) {
            markup.add(setEpigraphStyle().jm);
        } else if ("image".equals(qName)) {
            final String ref = attributes.getValue("href");
            if (cover) {
                document.setCover(ref);
            } else {
                markup.add(new FB2MarkupImageRef(ref, paragraphParsing));
            }
        } else if ("coverpage".equals(qName)) {
            cover = true;
        }
    }

    private FB2MarkupElement emptyLine(final int textSize) {
        return new FB2LineWhiteSpace(FB2Page.PAGE_WIDTH - 2 * FB2Page.MARGIN_X, crs.textSize, true);
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        spaceNeeded = true;
        if ("p".equals(qName) || "v".equals(qName)) {
            if (parsingNotesP) {
                parsingNotesP = false;
                final FB2Line line = FB2Line.getLastLine(noteLines);
                line.append(new FB2LineWhiteSpace(FB2Page.PAGE_WIDTH - line.width - 2 * FB2Page.MARGIN_X, crs.textSize,
                        false));
                for (final FB2Line l : noteLines) {
                    l.applyJustification(JustificationMode.Justify);
                }
            } else {
                markup.add(FB2MarkupParagraphEnd.E);
                paragraphParsing = false;
            }
        } else if ("binary".equals(qName)) {
            document.addImage(tmpBinaryName, tmpBinaryContents.toString());
            tmpBinaryName = null;
            tmpBinaryContents.setLength(0);
            parsingBinary = false;
        } else if ("body".equals(qName)) {
            if (documentStarted && !documentEnded) {
                documentEnded = true;
            }
            parsingNotes = false;
        } else if ("section".equals(qName)) {
            if (parsingNotes) {
                document.addNote(noteName, noteLines);
                noteLines = null;
                noteId = -1;
                noteFirstWord = true;
            } else {
                markup.add(new FB2MarkupEndPage());
                inSection = false;
            }
        } else if ("title".equals(qName)) {
            inTitle = false;
            if (!parsingNotes) {
                markup.add(new FB2MarkupTitle(title.toString()));
                markup.add(emptyLine(crs.textSize));
                markup.add(setPrevStyle().jm);
            }
        } else if ("cite".equals(qName)) {
            inCite = false;
            if (!parsingNotes) {
                markup.add(emptyLine(crs.textSize));
                markup.add(setPrevStyle().jm);
            }
        } else if ("subtitle".equals(qName)) {
            if (!parsingNotes) {
                markup.add(FB2MarkupParagraphEnd.E);
                markup.add(emptyLine(crs.textSize));
                markup.add(setPrevStyle().jm);
                paragraphParsing = false;
            }
        } else if ("text-author".equals(qName)) {
            if (!parsingNotes) {
                markup.add(FB2MarkupParagraphEnd.E);
                markup.add(setPrevStyle().jm);
                paragraphParsing = false;
            }
        } else if ("stanza".equals(qName)) {
            if (!parsingNotes) {
                markup.add(emptyLine(crs.textSize));
            }
        } else if ("poem".equals(qName)) {
            if (!parsingNotes) {
                markup.add(emptyLine(crs.textSize));
                markup.add(setPrevStyle().jm);
            }
        } else if ("strong".equals(qName)) {
            setPrevStyle();
        } else if ("sup".equals(qName)) {
            setPrevStyle();
            if (markup.get(markup.size() - 1) instanceof FB2MarkupNoSpace) {
                markup.remove(markup.size() - 1);
            }
        } else if ("sub".equals(qName)) {
            setPrevStyle();
            if (markup.get(markup.size() - 1) instanceof FB2MarkupNoSpace) {
                markup.remove(markup.size() - 1);
            }
        } else if ("emphasis".equals(qName)) {
            setPrevStyle();
        } else if ("epigraph".equals(qName)) {
            markup.add(setPrevStyle().jm);
        } else if ("coverpage".equals(qName)) {
            cover = false;
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        if (!(documentStarted && !documentEnded) && !paragraphParsing && !parsingBinary && !parsingNotes) {
            return;
        }
        if (parsingBinary) {
            tmpBinaryContents.append(ch, start, length);
        } else if (parsingNotesP && noteLines != null) {
            final int space = (int) crs.getTextPaint().measureText(" ");
            final int count = StringUtils.split(ch, start, length, starts, lengths);

            if (count > 0) {
                final char[] dst = new char[length];
                System.arraycopy(ch, start, dst, 0, length);

                for (int i = 0; i < count; i++) {
                    final int st = starts[i];
                    final int len = lengths[i];
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
                    final FB2TextElement te = new FB2TextElement(dst, st - start, len, crs);
                    FB2Line line = FB2Line.getLastLine(noteLines);
                    if (line.width + 2 * FB2Page.MARGIN_X + space + te.width < FB2Page.PAGE_WIDTH) {
                        if (line.hasNonWhiteSpaces()) {
                            line.append(new FB2LineWhiteSpace(space, crs.textSize, true));
                        }
                    } else {
                        line = new FB2Line();
                        noteLines.add(line);
                    }
                    line.append(te);
                }
            }
        } else if (documentStarted && !documentEnded) {
            if (inTitle) {
                title.append(ch, start, length);
            }
            final int count = StringUtils.split(ch, start, length, starts, lengths);

            if (count > 0) {
                if (!spaceNeeded) {
                    markup.add(FB2MarkupNoSpace._instance);
                }
                spaceNeeded = true;
                final char[] dst = new char[length];
                System.arraycopy(ch, start, dst, 0, length);

                for (int i = 0; i < count; i++) {
                    final int st = starts[i];
                    final int len = lengths[i];
                    markup.add(new FB2TextElement(dst, st - start, len, crs));
                    if (crs.isSuperScript() || crs.isSubScript()) {
                        markup.add(FB2MarkupNoSpace._instance);
                    }
                }
                if (Character.isWhitespace(ch[start + length - 1])) {
                    markup.add(FB2MarkupNoSpace._instance);
                    markup.add(new FB2LineWhiteSpace((int) crs.getTextPaint().measureText(" "), crs.textSize, true));
                }
                spaceNeeded = false;
            }
        }
    }

    private String getNoteId() {
        final Matcher matcher = notesPattern.matcher(noteName);
        String n = noteName;
        if (matcher.matches()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (matcher.group(i) != null) {
                    noteId = Integer.parseInt(matcher.group(i));
                    n = "" + noteId + ")";
                    break;
                }
                noteId = -1;
            }
        }
        return n;
    }

}
