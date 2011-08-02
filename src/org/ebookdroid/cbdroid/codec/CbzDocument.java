package org.ebookdroid.cbdroid.codec;

import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.utils.archives.zip.ZipArchive;
import org.ebookdroid.core.utils.archives.zip.ZipArchiveEntry;

import java.io.File;
import java.io.IOException;

public class CbzDocument extends AbsrtractCbxDocument<ZipArchive, ZipArchiveEntry> {

    public CbzDocument(final String fileName) {
        super(fileName);
    }

    @Override
    protected ZipArchive createArchive(File file) throws IOException {
        return new ZipArchive(file);
    }

    @Override
    public CodecPageInfo getPageInfo(final int pageIndex, final CodecContext codecContext) {
        return getPage(pageIndex).getPageInfo();
    }

    public static CodecDocument openDocument(final String fileName) {
        return new CbzDocument(fileName);
    }

}
