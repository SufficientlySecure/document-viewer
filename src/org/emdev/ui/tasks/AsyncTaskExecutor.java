package org.emdev.ui.tasks;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.emdev.common.android.AndroidVersion;
import org.emdev.utils.collections.ArrayDeque;

public class AsyncTaskExecutor {

    static final AsyncTaskExecutor DEFAULT = new AsyncTaskExecutor(10, 5, 128, 1, "AsyncTask");

    static final boolean USE_PARALLEL_AS_DEFAULT = AndroidVersion.lessThan3x;

    private final ThreadFactory threadFactory;
    private final BlockingQueue<Runnable> poolWorkQueue;
    private final Executor executor;
    private final SerialExecutor serial;

    public AsyncTaskExecutor(final int maxQueueSize, final int corePoolSize, final int maximumPoolSize,
            final long keepAliveTime, final String threadName) {
        threadFactory = new DefaultThreadFactory(threadName);
        poolWorkQueue = new ArrayBlockingQueue<Runnable>(maxQueueSize);
        executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS,
                poolWorkQueue, threadFactory);
        serial = new SerialExecutor();
    }

    @SuppressWarnings("unchecked")
    public <Params, Progress, Result, Task extends AsyncTask<Params, Progress, Result>> Task execute(final Task task,
            final Params... params) {
        return (Task) task.executeOnExecutor(executor, params);
    }

    @SuppressWarnings("unchecked")
    <Params, Progress, Result, Task extends AsyncTask<Params, Progress, Result>> Task executeAsDefault(final Task task,
            final Params... params) {
        return (Task) task.executeOnExecutor(USE_PARALLEL_AS_DEFAULT ? executor : serial, params);
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

    private class SerialExecutor implements Executor {

        final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();
        Runnable mActive;

        @Override
        public synchronized void execute(final Runnable r) {
            mTasks.offer(new Runnable() {

                @Override
                public void run() {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                }
            });
            if (mActive == null) {
                scheduleNext();
            }
        }

        protected synchronized void scheduleNext() {
            if ((mActive = mTasks.poll()) != null) {
                executor.execute(mActive);
            }
        }
    }
}
