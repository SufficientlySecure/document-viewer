package org.ebookdroid.fb2droid.codec;

import org.ebookdroid.core.OutlineLink;
import org.ebookdroid.core.PageLink;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.utils.archives.zip.ZipArchive;
import org.ebookdroid.core.utils.archives.zip.ZipArchiveEntry;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

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
import java.util.TreeMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;

public class FB2Document implements CodecDocument {

    private final TreeMap<String, FB2Image> images = new TreeMap<String, FB2Image>();
    private final TreeMap<String, ArrayList<FB2Line>> notes = new TreeMap<String, ArrayList<FB2Line>>();

    JustificationMode jm = JustificationMode.Justify;

    private final ArrayList<FB2Page> pages = new ArrayList<FB2Page>();
    private final ArrayList<FB2Line> paragraphLines = new ArrayList<FB2Line>();

    private String cover;

    private final List<OutlineLink> outline = new ArrayList<OutlineLink>();

    boolean insertSpace = true;

    public FB2Document(final String fileName) {
        final SAXParserFactory spf = SAXParserFactory.newInstance();

        final long t2 = System.currentTimeMillis();
        final List<FB2MarkupElement> markup = parseContent(spf, fileName);
        final long t3 = System.currentTimeMillis();
        System.out.println("SAX parser: " + (t3 - t2) + " ms");
        System.out.println("Words=" + FB2Words.words + ", uniques=" + FB2Words.uniques);
        createDocumentMarkup(markup);

        final long t4 = System.currentTimeMillis();
        System.out.println("Markup: " + (t4 - t3) + " ms");
    }

    private void createDocumentMarkup(final List<FB2MarkupElement> markup) {
        pages.clear();
        jm = JustificationMode.Justify;
        for (final FB2MarkupElement me : markup) {
            if (me instanceof FB2MarkupEndDocument) {
                break;
            }
            me.publishToDocument(this);
        }
        markup.clear();
        notes.clear();
    }

    private List<FB2MarkupElement> parseContent(final SAXParserFactory spf, final String fileName) {
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
                    final char c = (char) inStream.read();
                    buffer[len++] = c;
                    if (c == '>') {
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
            for (Closeable r : resources) {
                try {
                    if (r != null) {
                        r.close();
                    }
                } catch (IOException e) {
                }
            }
            resources.clear();
        }
        return h.markup;
    }

    void appendLine(final FB2Line line) {
        FB2Page lastPage = FB2Page.getLastPage(pages);

        if (lastPage.contentHeight + 2 * FB2Page.MARGIN_Y + line.getTotalHeight() > FB2Page.PAGE_HEIGHT) {
            commitPage();
            lastPage = new FB2Page();
            pages.add(lastPage);
        }
        lastPage.appendLine(line);
        final List<FB2Line> footnotes = line.getFootNotes();
        if (footnotes != null) {
            Iterator<FB2Line> iterator = footnotes.iterator();
            if (lastPage.noteLines.size() > 0 && iterator.hasNext()) {
                // Skip rule for non first note on page
                iterator.next();
            }
            while (iterator.hasNext()) {
                FB2Line l = iterator.next();
                lastPage = FB2Page.getLastPage(pages);
                if (lastPage.contentHeight + 2 * FB2Page.MARGIN_Y + l.getTotalHeight() > FB2Page.PAGE_HEIGHT) {
                    commitPage();
                    lastPage = new FB2Page();
                    pages.add(lastPage);
                    final FB2Line ruleLine = new FB2Line();
                    ruleLine.append(new FB2HorizontalRule(FB2Page.PAGE_WIDTH / 4, RenderingStyle.FOOTNOTE_SIZE));
                    ruleLine.applyJustification(JustificationMode.Left);
                    lastPage.appendNoteLine(ruleLine);
                }
                lastPage.appendNoteLine(l);
            }
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
    public CodecPageInfo getPageInfo(final int pageNuber) {
        final CodecPageInfo codecPageInfo = new CodecPageInfo();
        codecPageInfo.width = FB2Page.PAGE_WIDTH;
        codecPageInfo.height = FB2Page.PAGE_HEIGHT;
        return codecPageInfo;
    }

    @Override
    public List<PageLink> getPageLinks(final int pageNuber) {
        return null;
    }

    @Override
    public boolean isRecycled() {
        return false;
    }

    @Override
    public void recycle() {
    }

    void commitPage() {
        final FB2Page lastPage = FB2Page.getLastPage(pages);
        lastPage.commit();
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

    public void addNote(final String noteName, final ArrayList<FB2Line> noteLines) {
        if (noteName != null && noteLines != null) {
            notes.put(noteName, noteLines);
        }
    }

    public List<FB2Line> getNote(final String noteName) {
        List<FB2Line> note = notes.get(noteName);
        if (note == null && noteName.startsWith("#")) {
            note = notes.get(noteName.substring(1));
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

    public void publishElement(final AbstractFB2LineElement le) {
        FB2Line line = FB2Line.getLastLine(paragraphLines);
        final FB2LineWhiteSpace space = RenderingStyle.getTextPaint(line.getHeight()).space;
        if (line.width + 2 * FB2Page.MARGIN_X + space.width + le.width < FB2Page.PAGE_WIDTH) {
            if (line.hasNonWhiteSpaces() && insertSpace) {
                line.append(space);
            }
        } else {
            line = new FB2Line();
            paragraphLines.add(line);
        }
        line.append(le);
        insertSpace = true;
    }

    public void commitParagraph() {
        if (paragraphLines.isEmpty()) {
            return;
        }
        final FB2Line last = FB2Line.getLastLine(paragraphLines);
        final JustificationMode lastJm = jm == JustificationMode.Justify ? JustificationMode.Left : jm;
        for (final FB2Line l : paragraphLines) {
            l.applyJustification(l != last ? jm : lastJm);
            appendLine(l);
        }
        paragraphLines.clear();
    }

    public void publishImage(final String ref, final boolean inline) {
        final FB2Image image = getImage(ref, inline);
        if (image != null) {
            if (!inline) {
                final FB2Line line = new FB2Line();
                line.append(image);
                line.applyJustification(JustificationMode.Center);
                appendLine(line);
            } else {
                publishElement(image);
            }
        }
    }

    public void publishNote(final String ref) {
        final List<FB2Line> note = getNote(ref);
        if (note != null && !paragraphLines.isEmpty()) {
            final FB2Line line = FB2Line.getLastLine(paragraphLines);
            line.addNote(note);
        }
    }

    public void addTitle(final FB2MarkupTitle title) {
        outline.add(new OutlineLink(title.title, "#" + pages.size(), title.level));
    }

}
