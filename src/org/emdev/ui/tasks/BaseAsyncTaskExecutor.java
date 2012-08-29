package org.emdev.ui.tasks;

import android.os.AsyncTask;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BaseAsyncTaskExecutor {

    private final ThreadFactory threadFactory;
    private final BlockingQueue<Runnable> poolWorkQueue;
    private final Executor executor;

    public BaseAsyncTaskExecutor(final int maxQueueSize, final int corePoolSize, final int maximumPoolSize,
            final long keepAliveTime, final String threadName) {
        threadFactory = new DefaultThreadFactory(threadName);
        poolWorkQueue = new ArrayBlockingQueue<Runnable>(maxQueueSize);
        executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS,
                poolWorkQueue, threadFactory);
    }

    @SuppressWarnings("unchecked")
    public <Params, Progress, Result, Task extends AsyncTask<Params, Progress, Result>> Task execute(final Task task,
            final Params... params) {
        return (Task) task.executeOnExecutor(executor, params);
    }

    @SuppressWarnings("unchecked")
    public <Params, Result, Task extends BaseAsyncTask<Params, Result>> Task execute(final Task task,
            final Params... params) {
        return (Task) task.executeOnExecutor(executor, params);
    }

    private static final class DefaultThreadFactory implements ThreadFactory {

        private final AtomicInteger mCount = new AtomicInteger(1);
        private final String threadName;

        private DefaultThreadFactory(final String threadName) {
            super();
            this.threadName = threadName;
        }

        @Override
        public Thread newThread(final Runnable r) {
            return new Thread(r, threadName + "-" + mCount.getAndIncrement());
        }
    }
}
