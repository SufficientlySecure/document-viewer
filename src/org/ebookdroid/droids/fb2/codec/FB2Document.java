package org.ebookdroid.droids.fb2.codec;

import static org.ebookdroid.droids.fb2.codec.FB2Page.MARGIN_X;
import static org.ebookdroid.droids.fb2.codec.FB2Page.MARGIN_Y;
import static org.ebookdroid.droids.fb2.codec.FB2Page.PAGE_HEIGHT;
import static org.ebookdroid.droids.fb2.codec.FB2Page.PAGE_WIDTH;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.emdev.utils.LengthUtils;
import org.emdev.utils.archives.zip.ZipArchive;
import org.emdev.utils.archives.zip.ZipArchiveEntry;
import org.xml.sax.InputSource;

public class FB2Document implements CodecDocument {



    public class ParsedContent {
        final ArrayList<FB2MarkupElement> docMarkup = new ArrayList<FB2MarkupElement>();

        final HashMap<String, ArrayList<FB2MarkupElement>> streams = new HashMap<String, ArrayList<FB2MarkupElement>>();
        String currentStream = null;

        public void clear() {
            docMarkup.clear();
            for (Entry<String, ArrayList<FB2MarkupElement>> entry : streams.entrySet()) {
                final ArrayList<FB2MarkupElement> value = entry.getValue();
                if (value != null) {
                    value.clear();
                }
            }
        }

        public ArrayList<FB2MarkupElement> getMarkupStream() {
            return getMarkupStream(currentStream);
        }

        public ArrayList<FB2MarkupElement> getMarkupStream(String streamName) {
            if (streamName == null) {
                return docMarkup;
            }
            ArrayList<FB2MarkupElement> stream = streams.get(streamName);
            if (stream == null) {
                stream = new ArrayList<FB2MarkupElement>();
                streams.put(streamName, stream);
            }
            return stream;
        }
    }

    public class LineCreationParams {

        public JustificationMode jm;
        public int maxLineWidth;
        public boolean insertSpace = true;
        public FB2Document doc;

    }

    private final TreeMap<String, FB2Image> images = new TreeMap<String, FB2Image>();
    private final TreeMap<String, ArrayList<FB2Line>> notes = new TreeMap<String, ArrayList<FB2Line>>();

    JustificationMode jm = JustificationMode.Justify;

    private final ArrayList<FB2Page> pages = new ArrayList<FB2Page>();

    private String cover;

    private final List<OutlineLink> outline = new ArrayList<OutlineLink>();

    private ParsedContent content;

    public FB2Document(final String fileName) {
        final SAXParserFactory spf = SAXParserFactory.newInstance();

        final long t2 = System.currentTimeMillis();
        content = parseContent(spf, fileName);
        final long t3 = System.currentTimeMillis();
        System.out.println("SAX parser: " + (t3 - t2) + " ms");
        System.out.println("Words=" + FB2Words.words + ", uniques=" + FB2Words.uniques);

        final List<FB2Line> documentLines = createLines(content.getMarkupStream(null), PAGE_WIDTH - 2 * MARGIN_X);
        createPages(documentLines);

        content.clear();

        System.gc();
        final long t4 = System.currentTimeMillis();
        System.out.println("Markup: " + (t4 - t3) + " ms");
    }

    private ArrayList<FB2Line> createLines(List<FB2MarkupElement> markup, int maxLineWidth) {
        ArrayList<FB2Line> lines = new ArrayList<FB2Line>();
        if (LengthUtils.isNotEmpty(markup)) {
            LineCreationParams params = new LineCreationParams();
            params.jm = JustificationMode.Justify;
            params.maxLineWidth = maxLineWidth;
            params.doc = this;
            for (final FB2MarkupElement me : markup) {
                if (me instanceof FB2MarkupEndDocument) {
                    break;
                }
                me.publishToLines(lines, params);
            }
        }
        return lines;
    }

    private void createPages(List<FB2Line> documentLines) {
        pages.clear();
        if (LengthUtils.isEmpty(documentLines)) {
            return;
        }

        for (FB2Line line : documentLines) {
            FB2Page lastPage = getLastPage();

            if (lastPage.contentHeight + 2 * MARGIN_Y + line.getTotalHeight() > PAGE_HEIGHT) {
                commitPage();
                lastPage = new FB2Page();
                pages.add(lastPage);
            }

            lastPage.appendLine(line);
            FB2MarkupTitle title = line.getTitle();
            if (title != null) {
                addTitle(title);
            }

            final List<FB2Line> footnotes = line.getFootNotes();
            if (footnotes != null) {
                final Iterator<FB2Line> iterator = footnotes.iterator();
                if (lastPage.noteLines.size() > 0 && iterator.hasNext()) {
                    // Skip rule for non first note on page
                    iterator.next();
                }
                while (iterator.hasNext()) {
                    final FB2Line l = iterator.next();
                    lastPage = getLastPage();
                    if (lastPage.contentHeight + 2 * MARGIN_Y + l.getTotalHeight() > PAGE_HEIGHT) {
                        commitPage();
                        lastPage = new FB2Page();
                        pages.add(lastPage);
                        final FB2Line ruleLine = new FB2Line(PAGE_WIDTH / 4);
                        ruleLine.append(new FB2HorizontalRule(PAGE_WIDTH / 4, FB2FontStyle.FOOTNOTE.getFontSize()));
                        ruleLine.applyJustification(JustificationMode.Left);
                        lastPage.appendNoteLine(ruleLine);
                    }
                    lastPage.appendNoteLine(l);
                }
            }
        }
    }

    private ParsedContent parseContent(final SAXParserFactory spf, final String fileName) {
        final FB2ContentHandler h = new FB2ContentHandler(this);
        final List<Closeable> resources = new ArrayList<Closeable>();

        try {
            InputStream inStream = null;

            if (fileName.endsWith("zip")) {
                final ZipArchive zipArchive = new ZipArchive(new File(fileName));

                final Enumeration<ZipArchiveEntry> entries = zipArchive.entries();
                while (entries.hasMoreElements()) {
                    final ZipArchiveEntry entry = entries.nextElement();
                    if (!entry.isDirectory() && entry.getName().endsWith("fb2")) {
                        inStream = entry.open();
                        resources.add(inStream);
                        break;
                    }
                }
                resources.add(zipArchive);
            } else {
                inStream = new FileInputStream(fileName);
                resources.add(inStream);
            }
            if (inStream != null) {

                String encoding = "utf-8";
                final char[] buffer = new char[256];
                boolean found = false;
                int len = 0;
                while (len < 256) {
                    final int val = inStream.read();
                    if (len == 0 && (val == 0xEF || val == 0xBB || val == 0xBF)) {
                        continue;
                    }
                    buffer[len++] = (char) val;
                    if (val == '>') {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    final String xmlheader = new String(buffer, 0, len).trim();
                    if (xmlheader.startsWith("<?xml") && xmlheader.endsWith("?>")) {
                        final int index = xmlheader.indexOf("encoding");
                        if (index > 0) {
                            final int startIndex = xmlheader.indexOf('"', index);
                            if (startIndex > 0) {
                                final int endIndex = xmlheader.indexOf('"', startIndex + 1);
                                if (endIndex > 0) {
                                    encoding = xmlheader.substring(startIndex + 1, endIndex);
                                    System.out.println("XML encoding:" + encoding);
                                }
                            }
                        }
                    } else {
                        throw new RuntimeException("FB2 document can not be opened: " + "Invalid header");
                    }
                } else {
                    throw new RuntimeException("FB2 document can not be opened: " + "Header not found");
                }

                final Reader isr = new BufferedReader(new InputStreamReader(inStream, encoding), 32 * 1024);
                resources.add(isr);
                final InputSource is = new InputSource();
                is.setCharacterStream(isr);
                final SAXParser parser = spf.newSAXParser();
                parser.parse(is, h);
            }
        } catch (final StopParsingException e) {
            // do nothing
        } catch (final Exception e) {
            throw new RuntimeException("FB2 document can not be opened: " + e.getMessage(), e);
        } finally {
            for (final Closeable r : resources) {
                try {
                    if (r != null) {
                        r.close();
                    }
                } catch (final IOException e) {
                }
            }
            resources.clear();
        }
        return h.parsedContent;
    }

    @Override
    public List<OutlineLink> getOutline() {
        return outline;
    }

    @Override
    public CodecPage getPage(final int pageNuber) {
        if (0 <= pageNuber && pageNuber < pages.size()) {
            return pages.get(pageNuber);
        } else {
            return null;
        }
    }

    @Override
    public int getPageCount() {
        return pages.size();
    }

    @Override
    public CodecPageInfo getUnifiedPageInfo() {
        return FB2Page.CPI;
    }

    @Override
    public CodecPageInfo getPageInfo(final int pageNuber) {
        return FB2Page.CPI;
    }

    @Override
    public boolean isRecycled() {
        return false;
    }

    @Override
    public void recycle() {
    }

    void commitPage() {
        getLastPage().commit();
    }

    public void addImage(final String tmpBinaryName, final String encoded) {
        if (tmpBinaryName != null && encoded != null) {
            images.put("I" + tmpBinaryName, new FB2Image(encoded, true));
            images.put("O" + tmpBinaryName, new FB2Image(encoded, false));
        }
    }

    public FB2Image getImage(final String name, final boolean inline) {
        if (name == null) {
            return null;
        }
        FB2Image img = images.get((inline ? "I" : "O") + name);
        if (img == null && name.startsWith("#")) {
            img = images.get((inline ? "I" : "O") + name.substring(1));
        }
        return img;
    }

    public List<FB2Line> getNote(final String noteName) {
        ArrayList<FB2Line> note = notes.get(noteName);
        if (note != null) {
            return note;
        }
        if (content != null) {
            ArrayList<FB2MarkupElement> stream = content.getMarkupStream(noteName);
            if (LengthUtils.isEmpty(stream) && noteName.startsWith("#")) {
                stream = content.getMarkupStream(noteName.substring(1));
            }
            if (stream != null) {
                note = createLines(stream, PAGE_WIDTH - 2 * MARGIN_X);
                notes.put(noteName, note);
            }
        }

        return note;
    }

    public void setCover(final String value) {
        this.cover = value;
    }

    @Override
    public Bitmap getEmbeddedThumbnail() {
        final FB2Image image = getImage(cover, false);
        if (image != null) {
            final byte[] data = image.getData();
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        }
        return null;
    }


    public void addTitle(final FB2MarkupTitle title) {
        outline.add(new OutlineLink(title.title, "#" + pages.size(), title.level));
    }

    @Override
    public List<? extends RectF> searchText(final int pageNuber, final String pattern) throws DocSearchNotSupported {
        if (LengthUtils.isEmpty(pattern)) {
            return null;
        }

        final FB2Page page = (FB2Page) getPage(pageNuber);
        if (page == null) {
            return null;
        }

        final List<RectF> rects = new ArrayList<RectF>();

        final char[] charArray = pattern.toCharArray();
        final float y = searchText(page.lines, charArray, rects, FB2Page.MARGIN_Y);

        searchText(page.noteLines, charArray, rects, y);

        return rects;
    }

    private float searchText(final ArrayList<FB2Line> lines, final char[] pattern, final List<RectF> rects, float y) {
        for (int i = 0, n = lines.size(); i < n; i++) {
            final FB2Line line = lines.get(i);
            final float top = y;
            final float bottom = y + line.getHeight();
            line.ensureJustification();
            float x = FB2Page.MARGIN_X;
            for (int i1 = 0, n1 = line.elements.size(); i1 < n1; i1++) {
                final AbstractFB2LineElement e = line.elements.get(i1);
                final float w = e.width + (e instanceof FB2LineWhiteSpace ? line.spaceWidth : 0);
                if (e instanceof FB2TextElement) {
                    final FB2TextElement textElement = (FB2TextElement) e;
                    if (textElement.indexOf(pattern) != -1) {
                        rects.add(new RectF(x / FB2Page.PAGE_WIDTH, top / FB2Page.PAGE_HEIGHT, (x + w)
                                / FB2Page.PAGE_WIDTH, bottom / FB2Page.PAGE_HEIGHT));
                    }
                }
                x += w;
            }
            y = bottom;
        }
        return y;
    }

    public FB2Page getLastPage() {
        if (pages.size() == 0) {
            pages.add(new FB2Page());
        }
        FB2Page fb2Page = pages.get(pages.size() - 1);
        if (fb2Page.committed) {
            fb2Page = new FB2Page();
            pages.add(fb2Page);
        }
        return fb2Page;
    }
}
