package org.ebookdroid.ui.opds.adapters;

import org.ebookdroid.opds.model.Book;
import org.ebookdroid.opds.model.BookDownloadLink;

import java.io.File;

import org.emdev.ui.tasks.BaseFileAsyncTask.FileTaskResult;

class DownloadBookResult extends FileTaskResult {

    public Book book;
    public BookDownloadLink link;

    public DownloadBookResult(final Book book, final BookDownloadLink link, final File target) {
        super(target);
        this.book = book;
        this.link = link;
    }

    public DownloadBookResult(final Book book, final BookDownloadLink link, final Throwable error) {
        super(error);
        this.book = book;
        this.link = link;
    }
}