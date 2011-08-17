package org.ebookdroid.core;

import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DecodeServiceBase implements DecodeService {

    public static final String DECODE_SERVICE = "ViewDroidDecodeService";

    static final int PAGE_POOL_SIZE = 16;

    static final AtomicLong TASK_ID_SEQ = new AtomicLong();

    final CodecContext codecContext;

    final Executor executor = new Executor();

    final AtomicBoolean isRecycled = new AtomicBoolean();

    CodecDocument document;

    final Map<Integer, SoftReference<CodecPage>> pages = new LinkedHashMap<Integer, SoftReference<CodecPage>>() {

        private static final long serialVersionUID = -8845124816503128098L;

        @Override
        protected boolean removeEldestEntry(final Entry<Integer, SoftReference<CodecPage>> eldest) {
            if (this.size() > PAGE_POOL_SIZE) {
                final CodecPage codecPage = eldest.getValue().get();
                if (codecPage != null) {
                    Log.d(DECODE_SERVICE, "Recycling old page: " + codecPage);
                    codecPage.recycle();
                }
                return true;
            }
            return false;
        }

    };

    public DecodeServiceBase(final CodecContext codecContext) {
        this.codecContext = codecContext;
    }

    @Override
    public void open(final String fileName, final String password) {
        document = codecContext.openDocument(fileName, password);
    }

    @Override
    public void decodePage(final PageTreeNode node, final int targetWidth, final float zoom,
            final DecodeCallback decodeCallback) {
        final DecodeTask decodeTask = new DecodeTask(decodeCallback, targetWidth, zoom, node);

        if (isRecycled.get()) {
            Log.d(DECODE_SERVICE, "Decoding not allowed on recycling");
            return;
        }

        executor.add(decodeTask);
    }

    @Override
    public void stopDecoding(final PageTreeNode node, final String reason) {
        executor.stopDecoding(null, node, reason);
    }

    private void performDecode(final DecodeTask task) throws IOException {
        if (executor.isTaskDead(task)) {
            Log.d(DECODE_SERVICE, "Task " + task.id + ": Skipping dead decode task");
            return;
        }

        Log.d(DECODE_SERVICE, "Task " + task.id + ": Starting decoding");

        final CodecPage vuPage = getPage(task.pageNumber);

        if (executor.isTaskDead(task)) {
            Log.d(DECODE_SERVICE, "Task " + task.id + ": Abort dead decode task");
            return;
        }

        Log.d(DECODE_SERVICE, "Task " + task.id + ": Start converting map to bitmap");
        final float scale = calculateScale(vuPage, task.targetWidth) * task.zoom;
        final Bitmap bitmap = vuPage.renderBitmap(getScaledWidth(task, vuPage, scale),
                getScaledHeight(task, vuPage, scale), task.pageSliceBounds);
        Log.d(DECODE_SERVICE, "Task " + task.id + ": Converting map to bitmap finished");

        if (executor.isTaskDead(task)) {
            Log.d(DECODE_SERVICE, "Task " + task.id + ": Abort dead decode task");
            bitmap.recycle();
            return;
        }

        Log.d(DECODE_SERVICE, "Task " + task.id + ": Finish decoding task");
        finishDecoding(task, vuPage, bitmap);
    }

    private int getScaledHeight(final DecodeTask currentDecodeTask, final CodecPage vuPage, final float scale) {
        return Math.round(getScaledHeight(vuPage, scale) * currentDecodeTask.pageSliceBounds.height());
    }

    private int getScaledWidth(final DecodeTask currentDecodeTask, final CodecPage vuPage, final float scale) {
        return Math.round(getScaledWidth(vuPage, scale) * currentDecodeTask.pageSliceBounds.width());
    }

    private int getScaledHeight(final CodecPage vuPage, final float scale) {
        return (int) (scale * vuPage.getHeight());
    }

    private int getScaledWidth(final CodecPage vuPage, final float scale) {
        return (int) (scale * vuPage.getWidth());
    }

    private float calculateScale(final CodecPage codecPage, final int targetWidth) {
        return 1.0f * targetWidth / codecPage.getWidth();
    }

    private void finishDecoding(final DecodeTask currentDecodeTask, final CodecPage page, final Bitmap bitmap) {
        stopDecoding(currentDecodeTask.node, "complete");
        updateImage(currentDecodeTask, page, bitmap);
    }

    private CodecPage getPage(final int pageIndex) {
        final SoftReference<CodecPage> ref = pages.get(pageIndex);
        CodecPage page = ref != null ? ref.get() : null;
        if (page == null) {
            page = document.getPage(pageIndex);
            pages.put(pageIndex, new SoftReference<CodecPage>(page));
        }
        return page;
    }

    private void updateImage(final DecodeTask currentDecodeTask, final CodecPage page, final Bitmap bitmap) {
        currentDecodeTask.decodeCallback.decodeComplete(page, bitmap);
    }

    @Override
    public int getPageCount() {
        return document.getPageCount();
    }

    @Override
    public List<OutlineLink> getOutline() {
        return document.getOutline();
    }

    @Override
    public void recycle() {
        if (isRecycled.compareAndSet(false, true)) {
            executor.recycle();
        }
    }

    private class Executor implements RejectedExecutionHandler {

        final Map<PageTreeNode, DecodeTask> decodingTasks = new IdentityHashMap<PageTreeNode, DecodeTask>();
        final Map<Long, Future<?>> decodingFutures = new HashMap<Long, Future<?>>();

        final BlockingQueue<Runnable> queue;
        final ThreadPoolExecutor executorService;

        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        public Executor() {
            queue = new LinkedBlockingQueue<Runnable>();
            executorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, queue);
            executorService.setRejectedExecutionHandler(this);
        }

        public void add(final DecodeTask task) {
            lock.writeLock().lock();
            try {
                Log.d(DECODE_SERVICE, "Adding decoding task: " + task);

                final DecodeTask running = decodingTasks.get(task.node);

                if (running != null && running.equals(task) && !isTaskDead(running)) {
                    Log.d(DECODE_SERVICE, "The similar task is running: " + running.id);
                    return;
                } else if (running != null) {
                    Log.d(DECODE_SERVICE, "The another task is running: " + running.id);
                }

                decodingTasks.put(task.node, task);

                decodingFutures.put(task.id, executorService.submit(task));

                if (running != null) {
                    stopDecoding(running, null, "canceled by new one");
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        public void stopDecoding(final DecodeTask task, final PageTreeNode node, final String reason) {
            lock.writeLock().lock();
            try {
                final DecodeTask removed = task == null ? decodingTasks.remove(node) : task;
                final Future<?> future = removed != null ? decodingFutures.remove(removed.id) : null;

                if (removed != null) {
                    Log.d(DECODE_SERVICE, "Stop decoding task: " + removed.id + " with reason: " + reason);
                }
                if (future != null) {
                    if (removed == null) {
                        Log.d(DECODE_SERVICE, "Stop decoding task for " + node + " with reason: " + reason);
                    }
                    future.cancel(false);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        public boolean isTaskDead(final DecodeTask task) {
            lock.readLock().lock();
            try {
                final Future<?> future = decodingFutures.get(task.id);
                if (future != null) {
                    return future.isCancelled();
                }
                return true;
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
            if (r instanceof DecodeTask) {
                stopDecoding(((DecodeTask) r), null, "rejected by executor");
            }
        }

        public void recycle() {
            lock.writeLock().lock();
            try {
                for (final DecodeTask task : decodingTasks.values()) {
                    stopDecoding(task, null, "recycling");
                }
                executorService.submit(new Runnable() {

                    @Override
                    public void run() {
                        for (final SoftReference<CodecPage> codecPageSoftReference : pages.values()) {
                            final CodecPage page = codecPageSoftReference.get();
                            if (page != null) {
                                page.recycle();
                            }
                        }
                        if (document != null) {
                            document.recycle();
                        }
                        codecContext.recycle();
                        executorService.shutdown();
                    }
                });
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    private class DecodeTask implements Runnable {

        final long id = TASK_ID_SEQ.incrementAndGet();
        final PageTreeNode node;
        final int pageNumber;
        final float zoom;
        final DecodeCallback decodeCallback;
        final RectF pageSliceBounds;
        final int targetWidth;

        private DecodeTask(final DecodeCallback decodeCallback, final int targetWidth, final float zoom,
                final PageTreeNode node) {
            this.pageNumber = node.getDocumentPageIndex();
            this.decodeCallback = decodeCallback;
            this.zoom = zoom;
            this.node = node;
            this.pageSliceBounds = node.getPageSliceBounds();
            this.targetWidth = targetWidth;
        }

        @Override
        public void run() {
            try {
                Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1);
                performDecode(this);
            } catch (final IOException e) {
                Log.e(DECODE_SERVICE, "Decode fail", e);
            }
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof DecodeTask) {
                final DecodeTask that = (DecodeTask) obj;
                return this.pageNumber == that.pageNumber && this.pageSliceBounds.equals(that.pageSliceBounds)
                        && this.targetWidth == that.targetWidth && this.zoom == that.zoom;
            }
            return false;
        }

        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder("DecodeTask");
            buf.append("[");

            buf.append("id").append("=").append(id);
            buf.append(", ");
            buf.append("target").append("=").append(node);
            buf.append(", ");
            buf.append("width").append("=").append(targetWidth);
            buf.append(", ");
            buf.append("zoom").append("=").append(zoom);
            buf.append(", ");
            buf.append("bounds").append("=").append(pageSliceBounds);

            buf.append("]");
            return buf.toString();
        }
    }

    @Override
    public CodecPageInfo getPageInfo(final int pageIndex) {
        return document.getPageInfo(pageIndex);
    }

}
