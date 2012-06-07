package org.ebookdroid.opds.model;

import java.util.List;

public class Book extends Entry {

    public Author author;
    public Link thumbnail;
    public List<BookDownloadLink> downloads;

    public Book(final Feed parent, final String id, final String title, final Content content) {
        super(parent, id, title, content);
    }

}
