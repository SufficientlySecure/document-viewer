package org.ebookdroid.opds.model;

import java.util.ArrayList;
import java.util.List;

public class Feed extends Entry {

    public Link link;
    public Feed next;
    public Feed prev;
    public long loadedAt;

    public final List<Feed> facets = new ArrayList<Feed>();
    public final List<Feed> children = new ArrayList<Feed>();
    public final List<Book> books = new ArrayList<Book>();

    public Feed(final String title, final String uri) {
        this(null, uri, title, null);
        this.link = new Link(uri);
    }

    public Feed(final Feed parent, final String id, final String title, final Content content) {
        super(parent, id, title, content);
    }
}
