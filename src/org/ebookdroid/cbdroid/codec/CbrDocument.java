package org.ebookdroid.cbdroid.codec;

import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.utils.archives.rar.RarArchive;
import org.ebookdroid.core.utils.archives.rar.RarArchiveEntry;

import java.io.File;
import java.io.IOException;

public class CbrDocument extends AbsrtractCbxDocument<RarArchive, RarArchiveEntry> {

    public CbrDocument(final String fileName) {
        super(fileName);
    }

    @Override
    protected RarArchive createArchive(File file) throws IOException {
        return new RarArchive(file);
    }

    @Override
    public CodecPageInfo getPageInfo(final int pageIndex, final CodecContext codecContext) {
        return null;
    }

    public static CodecDocument openDocument(final String fileName) {
        return new CbrDocument(fileName);
    }
}
