package org.ebookdroid.opds;

import java.util.List;
import java.util.Map;

public interface IEntryBuilder {

    Feed newFeed(Feed parent, String id, String title, Content content, Link link, Map<String, Link> facets);

    Book newBook(Feed parent, String id, String title, Content content, Link thumbnail, List<BookDownloadLink> downloads);
}
