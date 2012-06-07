package org.ebookdroid.opds.model;

public class Link {

    public final LinkKind kind;
    public final String uri;
    public final String type;
    public final String rel;

    public Link(final LinkKind kind, final String uri, final String rel, final String type) {
        this.kind = kind;
        this.uri = uri;
        this.rel = rel;
        this.type = type;
    }

    public Link(final String uri) {
        this.kind = LinkKind.FEED;
        this.uri = uri;
        this.rel = null;
        this.type = "profile=opds-catalog";
    }
}
