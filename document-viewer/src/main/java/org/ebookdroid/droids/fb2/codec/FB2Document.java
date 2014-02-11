package org.ebookdroid.droids.fb2.codec;

import static org.ebookdroid.droids.fb2.codec.FB2Page.MARGIN_X;
import static org.ebookdroid.droids.fb2.codec.FB2Page.MARGIN_Y;
import static org.ebookdroid.droids.fb2.codec.FB2Page.PAGE_HEIGHT;
import static org.ebookdroid.droids.fb2.codec.FB2Page.PAGE_WIDTH;

import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.droids.fb2.codec.handlers.StandardHandler;
import org.ebookdroid.droids.fb2.codec.tags.FB2TagFactory;

import android.graphics.Bitmap;
import android.graphics.RectF;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicLong;

import org.emdev.common.archives.zip.ZipArchive;
import org.emdev.common.archives.zip.ZipArchiveEntry;
import org.emdev.common.textmarkup.JustificationMode;
import org.emdev.common.textmarkup.MarkupElement;
import org.emdev.common.textmarkup.MarkupTitle;
import org.emdev.common.textmarkup.TextStyle;
import org.emdev.common.textmarkup.line.HorizontalRule;
import org.emdev.common.textmarkup.line.Line;
import org.emdev.common.textmarkup.line.LineStream;
import org.emdev.common.xml.TextProvider;
import org.emdev.common.xml.parsers.DuckbillParser;
import org.emdev.common.xml.parsers.VTDExParser;
import org.emdev.utils.LengthUtils;

import com.ximpleware.VTDGenEx;

public class FB2Document extends AbstractCodecDocument {

    private final ArrayList<FB2Page> pages = new ArrayList<FB2Page>();

    private final List<OutlineLink> outline = new ArrayList<OutlineLink>();

    private final ParsedContent content = new ParsedContent();

    public FB2Document(final FB2Context context, final String fileName) {
        super(context, context.getContextHandle());

        final long t1 = System.currentTimeMillis();
        content.loadFonts();
        final long t2 = System.currentTimeMillis();
        System.out.println("Fonts preloading: " + (t2 - t1) + " ms");

        switch (AppSettings.current().fb2XmlParser) {
            case VTDEx:
                parseWithVTDEx(fileName);
                break;
            case Duckbill:
            default:
                parseWithDuckbill(fileName);
                break;
        }

        final long t3 = System.currentTimeMillis();

        final ArrayList<MarkupElement> mainStream = content.getMarkupStream(null);
        final LineStream documentLines = content.createLines(mainStream, PAGE_WIDTH - 2 * MARGIN_X,
                JustificationMode.Justify, AppSettings.current().fb2HyphenEnabled);
        createPages(documentLines);

        final long t4 = System.currentTimeMillis();
        System.out.println("Markup: " + (t4 - t3) + " ms");

        final int removed = removeEmptyPages(true);
        content.clear();

        final long t5 = System.currentTimeMillis();
        System.out.println("Cleanup: " + (t5 - t4) + " ms, removed: " + removed);
    }

    private void createPages(final LineStream documentLines) {
        pages.clear();
        if (LengthUtils.isEmpty(documentLines)) {
            return;
        }

        for (final Line line : documentLines) {
            FB2Page lastPage = getLastPage();

            if (lastPage.contentHeight + 2 * MARGIN_Y + line.getTotalHeight() > PAGE_HEIGHT) {
                commitPage();
                lastPage = new FB2Page();
                pages.add(lastPage);
            }

            lastPage.appendLine(line);
            final MarkupTitle title = line.getTitle();
            if (title != null) {
                addTitle(title);
            }

            final LineStream footnotes = line.getFootNotes();
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
                        final Line ruleLine = new Line(content, PAGE_WIDTH / 4, JustificationMode.Left);
                        ruleLine.append(new HorizontalRule(PAGE_WIDTH / 4, TextStyle.FOOTNOTE.getFontSize()));
                        ruleLine.applyJustification(JustificationMode.Left);
                        lastPage.appendNoteLine(ruleLine);
                    }
                    lastPage.appendNoteLine(l);
                }
            }
        }
        commitPage();
    }

    private int removeEmptyPages(final boolean all) {
        int count = 0;
        final ListIterator<FB2Page> i = pages.listIterator(pages.size());
        if (all) {
            while (i.hasPrevious()) {
                final FB2Page p = i.previous();
                if (p.isEmpty()) {
                    i.remove();
                    count++;
                }
            }
        } else {
            while (i.hasPrevious()) {
                final FB2Page p = i.previous();
                if (p.isEmpty()) {
                    i.remove();
                    count++;
                } else {
                    break;
                }
            }
        }
        return count;
    }

    private void parseWithVTDEx(final String fileName) {
        final StandardHandler h = new StandardHandler(content);
        final List<Closeable> resources = new ArrayList<Closeable>();

        final long t1 = System.currentTimeMillis();
        try {
            final AtomicLong size = new AtomicLong();
            final InputStream inStream = getInputStream(fileName, size, resources);

            if (inStream != null) {
                final TextProvider text = loadContent(inStream, size, resources);

                final long t2 = System.currentTimeMillis();
                System.out.println("VTDEx  load: " + (t2 - t1) + " ms");

                final VTDGenEx gen = new VTDGenEx();
                gen.setDoc(text.chars, 0, text.size);
                gen.parse(false);

                final long t3 = System.currentTimeMillis();
                System.out.println("VTDEx parse: " + (t3 - t2) + " ms");

                final VTDExParser p = new VTDExParser();
                p.parse(gen, FB2TagFactory.instance, h);

                final long t4 = System.currentTimeMillis();
                System.out.println("VTDEx  scan: " + (t4 - t3) + " ms");
            }
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

    private void parseWithDuckbill(final String fileName) {
        final StandardHandler h = new StandardHandler(content);
        final List<Closeable> resources = new ArrayList<Closeable>();

        final long t1 = System.currentTimeMillis();
        try {
            final AtomicLong size = new AtomicLong();
            final InputStream inStream = getInputStream(fileName, size, resources);

            if (inStream != null) {
                final TextProvider text = loadContent(inStream, size, resources);

                final long t2 = System.currentTimeMillis();
                System.out.println("DUCK  load: " + (t2 - t1) + " ms");

                final DuckbillParser p = new DuckbillParser();
                p.parse(text, FB2TagFactory.instance, h);

                final long t4 = System.currentTimeMillis();
                System.out.println("DUCK  parse: " + (t4 - t2) + " ms");
            }
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

    private TextProvider loadContent(final InputStream inStream, final AtomicLong size, final List<Closeable> resources)
            throws IOException, UnsupportedEncodingException {
        final String encoding = getEncoding(inStream);
        final Reader isr = new InputStreamReader(inStream, encoding);
        resources.add(isr);

        final char[] chars = new char[(int) size.get()];
        int offset = 0;
        for (int len = chars.length; offset < len;) {
            final int n = isr.read(chars, offset, len);
            if (n == -1) {
                break;
            }
            offset += n;
            len -= n;
        }
        size.set(offset);
        return new TextProvider(chars, offset);
    }

    private String getEncoding(final InputStream inStream) throws IOException {
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
        return encoding;
    }

    private InputStream getInputStream(final String fileName, final AtomicLong size, final List<Closeable> resources)
            throws IOException, FileNotFoundException {
        InputStream inStream = null;

        if (fileName.endsWith("zip")) {
            final ZipArchive zipArchive = new ZipArchive(new File(fileName));

            final Enumeration<ZipArchiveEntry> entries = zipArchive.entries();
            while (entries.hasMoreElements()) {
                final ZipArchiveEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith("fb2")) {
                    size.set(entry.getSize());
                    inStream = entry.open();
                    resources.add(inStream);
                    break;
                }
            }
            resources.add(zipArchive);
        } else {
            final File f = new File(fileName);
            size.set(f.length());
            inStream = new FileInputStream(f);
            resources.add(inStream);
        }
        return inStream;
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
    protected void freeDocument() {
        content.recycle();
        for (final FB2Page p : pages) {
            p.finalRecycle();
        }
        pages.clear();
    }

    void commitPage() {
        getLastPage().commit(content);
    }

    @Override
    public Bitmap getEmbeddedThumbnail() {
        return content.getCoverImage();
    }

    public void addTitle(final MarkupTitle title) {
        outline.add(new OutlineLink(title.title, "#" + pages.size(), title.level));
    }

    @Override
    public List<? extends RectF> searchText(final int pageNuber, final String pattern) {
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
