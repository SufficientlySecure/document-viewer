package org.ebookdroid.opds.model;

import org.ebookdroid.opds.OPDSBookType;

public class BookDownloadLink extends Link {

    public final OPDSBookType bookType;
    public final boolean isZipped;

    public BookDownloadLink(final LinkKind kind, final String uri, final String rel, final String type) {
        super(kind, uri, rel, type);
        this.bookType = OPDSBookType.getByMimeType(type);
        this.isZipped = OPDSBookType.isZippedContent(type);
    }
}
