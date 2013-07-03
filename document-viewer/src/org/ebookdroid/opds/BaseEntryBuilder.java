package org.ebookdroid.opds;

import org.ebookdroid.opds.model.Book;
import org.ebookdroid.opds.model.BookDownloadLink;
import org.ebookdroid.opds.model.Content;
import org.ebookdroid.opds.model.Feed;
import org.ebookdroid.opds.model.Link;

import java.util.List;
import java.util.Map;

public class BaseEntryBuilder implements IEntryBuilder {

    @Override
    public Feed newFeed(final Feed parent, final String id, final String title, final Content content, final Link link,
            final Map<String, Link> facets) {
        final Feed feed = new Feed(parent, id, title, content);
        feed.link = link;

        createFacets(feed, feed, facets);

        return feed;
    }

    @Override
    public Book newBook(final Feed parent, final String id, final String title, final Content content,
            final Link thumbnail, final List<BookDownloadLink> downloads) {
        final Book book = new Book(parent, id, title, content);
        book.thumbnail = thumbnail;
        book.downloads = downloads;
        return book;
    }

    protected void createFacets(final Feed facetParent, final Feed feed, final Map<String, Link> facets) {
        for (final Map.Entry<String, Link> f : facets.entrySet()) {
            final String ft = f.getKey();
            final Link fl = f.getValue();
            final Feed facet = new Feed(facetParent, fl.uri, ft, null);
            facet.link = fl;
            feed.facets.add(facet);
        }
    }
}
