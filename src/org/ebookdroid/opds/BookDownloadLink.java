package org.ebookdroid.opds;

public class BookDownloadLink extends Link {

    public final OPDSBookType bookType;
    public final boolean isZipped;
    
    public BookDownloadLink(LinkKind kind, String uri, String rel, String type) {
        super(kind, uri, rel, type);
        this.bookType = OPDSBookType.getByMimeType(type);
        this.isZipped = OPDSBookType.isZippedContent(type);
    }
}
