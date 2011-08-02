package org.ebookdroid.cbdroid.codec;

import org.ebookdroid.core.OutlineLink;
import org.ebookdroid.core.PageLink;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CbzDocument implements CodecDocument {

    private ZipFile zipfile;
    private final Map<String, ZipEntry> pages = new TreeMap<String, ZipEntry>();

    public CbzDocument(final String fileName) {
        try {
            zipfile = new ZipFile(fileName);
            init();
        } catch (final IOException e) {
        }
    }

    private void init() {
        if (zipfile != null) {
            final Enumeration<? extends ZipEntry> entries = zipfile.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()
                        && (entry.getName().toLowerCase().endsWith(".jpg") || entry.getName().toLowerCase()
                                .endsWith(".png"))) {
                    pages.put(entry.getName().toLowerCase(), entry);
                }
            }
        }
    }

    @Override
    public List<OutlineLink> getOutline() {
        return null;
    }

    @Override
    public CodecPage getPage(final int pageNumber) {
        final Collection<ZipEntry> values = pages.values();
        int index = 0;
        for (final ZipEntry zipEntry : values) {
            if (index++ == pageNumber) {
                return new CbzPage(zipfile, zipEntry);
            }
        }
        return new CbzPage(null, null);
    }

    @Override
    public int getPageCount() {
        return pages.size();
    }

    @Override
    public CodecPageInfo getPageInfo(final int pageIndex, final CodecContext codecContext) {
        return ((CbzPage) getPage(pageIndex)).getPageInfo();
    }

    @Override
    public ArrayList<PageLink> getPageLinks(final int pageNuber) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void recycle() {
        try {
            zipfile.close();
        } catch (final IOException e) {
        }

    }

    public static CodecDocument openDocument(final String fileName) {
        return new CbzDocument(fileName);
    }

}
