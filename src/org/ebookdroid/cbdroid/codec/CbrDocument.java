package org.ebookdroid.cbdroid.codec;

import org.ebookdroid.core.OutlineLink;
import org.ebookdroid.core.PageLink;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.innosystec.unrar.Archive;
import de.innosystec.unrar.exception.RarException;
import de.innosystec.unrar.rarfile.FileHeader;

public class CbrDocument implements CodecDocument {

    private Archive rarfile;
    private final Map<String, FileHeader> pages = new TreeMap<String, FileHeader>();

    public CbrDocument(final String fileName) {
        try {
            rarfile = new Archive(new File(fileName));
            init();
        } catch (final IOException e) {
        } catch (final RarException e) {
        }
    }

    private void init() {
        if (rarfile != null) {

            if (rarfile.isEncrypted()) {
                Log.d("CbrDocument", "Archive is encrypted. Cannot extract");
                return;
            }
            FileHeader fh = null;
            while (true) {
                fh = rarfile.nextFileHeader();
                if (fh == null) {
                    break;
                }
                if (fh.isEncrypted()) {
                    Log.d("CbrDocument", "File is encrypted. Cannot extract: " + fh.getFileNameString());
                    continue;
                }
                if (!fh.isDirectory()
                        && (fh.getFileNameString().toLowerCase().endsWith(".jpg") || fh.getFileNameString()
                                .toLowerCase().endsWith(".png"))) {
                    pages.put(fh.getFileNameString().toLowerCase(), fh);
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
        final Collection<FileHeader> values = pages.values();
        int index = 0;
        for (final FileHeader fh : values) {
            if (index++ == pageNumber) {
                return new CbrPage(rarfile, fh);
            }
        }
        return new CbrPage(null, null);
    }

    @Override
    public int getPageCount() {
        return pages.size();
    }

    @Override
    public CodecPageInfo getPageInfo(final int pageIndex, final CodecContext codecContext) {
        return null;
    }

    @Override
    public ArrayList<PageLink> getPageLinks(final int pageNuber) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void recycle() {
        try {
            rarfile.close();
        } catch (final IOException e) {
        }

    }

    public static CodecDocument openDocument(final String fileName) {
        return new CbrDocument(fileName);
    }
}
