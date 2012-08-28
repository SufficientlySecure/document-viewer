package org.ebookdroid.ui.opds.adapters;

import org.ebookdroid.opds.model.Book;
import org.ebookdroid.opds.model.BookDownloadLink;
import org.ebookdroid.opds.model.Feed;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class OPDSTaskExecutor {

    private static final int CORE_POOL_SIZE = 5;
    private static final int MAXIMUM_POOL_SIZE = 128;
    private static final int KEEP_ALIVE = 1;

    private static final ThreadFactory sThreadFactory = new OPDSThreadFactory();
    private static final BlockingQueue<Runnable> sPoolWorkQueue = new ArrayBlockingQueue<Runnable>(1024);

    /**
     * An {@link Executor} that can be used to execute tasks in parallel.
     */
    private static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
            KEEP_ALIVE, TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory);

    private final OPDSAdapter adapter;

    private volatile LoadThumbnailTask background;

    OPDSTaskExecutor(final OPDSAdapter adapter) {
        this.adapter = adapter;
    }

    public void startLoadFeed(final Feed feed) {
        if (feed != null) {
            if (feed.loadedAt == 0) {
                new LoadFeedTask(adapter).executeOnExecutor(THREAD_POOL_EXECUTOR, feed);
            } else {
                startLoadThumbnails(feed);
            }
        }
    }

    public void startBookDownload(final Book book, final BookDownloadLink link) {
        stopLoadThumbnails();
        new DownloadBookTask(adapter).executeOnExecutor(THREAD_POOL_EXECUTOR, book, link);
    }

    public void startLoadThumbnails(final Feed feed) {
        if (background != null) {
            background.cancel(true);
        }
        background = new LoadThumbnailTask(adapter);
        background.executeOnExecutor(THREAD_POOL_EXECUTOR, feed);
    }

    public void stopLoadThumbnails() {
        if (background != null) {
            background.cancel(true);
            background = null;
        }
    }

    private static final class OPDSThreadFactory implements ThreadFactory {

        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(final Runnable r) {
            return new Thread(r, "OPDSThread-" + mCount.getAndIncrement());
        }
    }
}
