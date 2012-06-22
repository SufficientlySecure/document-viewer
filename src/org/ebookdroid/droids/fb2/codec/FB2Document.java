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
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.emdev.utils.LengthUtils;
import org.emdev.utils.archives.zip.ZipArchive;
import org.emdev.utils.archives.zip.ZipArchiveEntry;
import org.emdev.utils.textmarkup.FontStyle;
import org.emdev.utils.textmarkup.JustificationMode;
import org.emdev.utils.textmarkup.MarkupTitle;
import org.emdev.utils.textmarkup.Words;
import org.emdev.utils.textmarkup.line.HorizontalRule;
import org.emdev.utils.textmarkup.line.Line;
import org.xml.sax.InputSource;

public class FB2Document implements CodecDocument {
    private final ArrayList<FB2Page> pages = new ArrayList<FB2Page>();

    private final List<OutlineLink> outline = new ArrayList<OutlineLink>();

    private final ParsedContent content = new ParsedContent();

    public FB2Document(final String fileName) {
        final SAXParserFactory spf = SAXParserFactory.newInstance();

        final long t2 = System.currentTimeMillis();
        parseContent(spf, fileName);
        final long t3 = System.currentTimeMillis();
        System.out.println("SAX parser: " + (t3 - t2) + " ms");
        System.out.println("Words=" + Words.words + ", uniques=" + Words.uniques);

        final List<Line> documentLines = content.createLines(content.getMarkupStream(null), PAGE_WIDTH - 2 * MARGIN_X, JustificationMode.Justify);
        createPages(documentLines);

        content.clear();

        System.gc();
        final long t4 = System.currentTimeMillis();
        System.out.println("Markup: " + (t4 - t3) + " ms");
    }


    private void createPages(List<Line> documentLines) {
        pages.clear();
        if (LengthUtils.isEmpty(documentLines)) {
            return;
        }

        for (Line line : documentLines) {
            FB2Page lastPage = getLastPage();

            if (lastPage.contentHeight + 2 * MARGIN_Y + line.getTotalHeight() > PAGE_HEIGHT) {
                commitPage();
                lastPage = new FB2Page();
                pages.add(lastPage);
            }

            lastPage.appendLine(line);
            MarkupTitle title = line.getTitle();
            if (title != null) {
                addTitle(title);
            }

            final List<Line> footnotes = line.getFootNotes();
            if (footnotes != null) {
                final Iterator<Line> iterator = footnotes.iterator();
                if (lastPage.noteLines.size() > 0 && iterator.hasNext()) {
                    // Skip rule for non first note on page
                    iterator.next();
                }
                while (iterator.hasNext()) {
                    final Line l = iterator.next();
                    lastPage = getLastPage();
                    if (lastPage.contentHeight + 2 * MARGIN_Y + l.getTotalHeight() > PAGE_HEIGHT) {
                        commitPage();
                        lastPage = new FB2Page();
                        pages.add(lastPage);
                        final Line ruleLine = new Line(PAGE_WIDTH / 4, JustificationMode.Left);
                        ruleLine.append(new HorizontalRule(PAGE_WIDTH / 4, FontStyle.FOOTNOTE.getFontSize()));
                        ruleLine.applyJustification(JustificationMode.Left);
                        lastPage.appendNoteLine(ruleLine);
                    }
                    lastPage.appendNoteLine(l);
                }
            }
        }
    }

    private void parseContent(final SAXParserFactory spf, final String fileName) {
        final FB2ContentHandler h = new FB2ContentHandler(content);
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

    @Override
    public Bitmap getEmbeddedThumbnail() {
        return content.getCoverImage();
    }


    public void addTitle(final MarkupTitle title) {
        outline.add(new OutlineLink(title.title, "#" + pages.size(), title.level));
    }

    @Override
    public List<? extends RectF> searchText(final int pageNuber, final String pattern) throws DocSearchNotSupported {
        if (LengthUtils.isEmpty(pattern)) {
            return null;
        }

        final FB2Page page = (FB2Page) getPage(pageNuber);
        return (page == null) ? null : page.searchText(pattern);

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
