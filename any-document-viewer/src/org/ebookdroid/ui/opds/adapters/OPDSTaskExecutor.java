package org.ebookdroid.ui.opds.adapters;

import org.ebookdroid.opds.model.Book;
import org.ebookdroid.opds.model.BookDownloadLink;
import org.ebookdroid.opds.model.Feed;

import org.emdev.ui.tasks.AsyncTaskExecutor;

public class OPDSTaskExecutor {

    private static final int CORE_POOL_SIZE = 1;
    private static final int MAXIMUM_POOL_SIZE = 10;
    private static final int KEEP_ALIVE = 1;

    private static final AsyncTaskExecutor executor = new AsyncTaskExecutor(1024, CORE_POOL_SIZE,
            MAXIMUM_POOL_SIZE, KEEP_ALIVE, "OPDSThread");

    private final OPDSAdapter adapter;

    private volatile LoadThumbnailTask background;

    OPDSTaskExecutor(final OPDSAdapter adapter) {
        this.adapter = adapter;
    }

    public void startLoadFeed(final Feed feed) {
        if (feed != null) {
            if (feed.loadedAt == 0) {
                executor.execute(new LoadFeedTask(adapter), feed);
            } else {
                startLoadThumbnails(feed);
            }
        }
    }

    public void startBookDownload(final Book book, final BookDownloadLink link) {
        stopLoadThumbnails();
        executor.execute(new DownloadBookTask(adapter), book, link);
    }

    public void startLoadThumbnails(final Feed feed) {
        if (background != null) {
            background.stop();
        }
        background = new LoadThumbnailTask(adapter);
        executor.execute(background, feed);
    }

    public void stopLoadThumbnails() {
        if (background != null) {
            background.cancel(true);
            background = null;
        }
    }
}
