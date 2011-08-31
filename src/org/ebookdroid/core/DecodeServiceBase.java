package org.ebookdroid.core;

import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.log.LogContext;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

import java.lang.ref.SoftReference;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DecodeServiceBase implements DecodeService {

    public static final LogContext LCTX = LogContext.ROOT.lctx("Decoding");

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
                SoftReference<CodecPage> value = eldest != null ? eldest.getValue() :null;
                final CodecPage codecPage = value != null ? value.get() : null;
                if (codecPage != null) {
                    if (LCTX.isDebugEnabled()) {
                        LCTX.d("Recycling old page: " + codecPage);
                    }
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
            final DecodeCallback decodeCallback, final boolean nativeResolution) {
        final DecodeTask decodeTask = new DecodeTask(decodeCallback, targetWidth, zoom, node, nativeResolution);

        if (isRecycled.get()) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Decoding not allowed on recycling");
            }
            return;
        }

        executor.add(decodeTask);
    }

    @Override
    public void stopDecoding(final PageTreeNode node, final String reason) {
        executor.stopDecoding(null, node, reason);
    }

    void performDecode(final DecodeTask task) {
        if (executor.isTaskDead(task)) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Task " + task.id + ": Skipping dead decode task for " + task.node);
            }
            return;
        }

        if (LCTX.isDebugEnabled()) {
            LCTX.d("Task " + task.id + ": Starting decoding for " + task.node);
        }

        CodecPage vuPage = null;
        try {
            vuPage = getPage(task.pageNumber);

            if (executor.isTaskDead(task)) {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("Task " + task.id + ": Abort dead decode task for " + task.node);
                }
                return;
            }

            final Rect r = getScaledSize(task, vuPage);
            LCTX.d("Rendering bitmap size: "+r);
            final Bitmap bitmap = vuPage.renderBitmap(r.width(), r.height(), task.pageSliceBounds);

            if (executor.isTaskDead(task)) {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("Task " + task.id + ": Abort dead decode task for " + task.node);
                }
                bitmap.recycle();
                return;
            }

            finishDecoding(task, vuPage, bitmap);
        } catch (OutOfMemoryError ex) {
            LCTX.e("Task " + task.id + ": No memory to decode " + task.node);
            for(int i=0; i < PAGE_POOL_SIZE; i++) {
                pages.put(Integer.MAX_VALUE - i, null);
            }
            vuPage.recycle();
            abortDecoding(task, null, null);
        } catch (Throwable th) {
            LCTX.e("Task " + task.id + ": Decoding failed for " + task.node + ": " + th.getMessage(), th);
            abortDecoding(task, vuPage, null);
        }
    }

    Rect getScaledSize(final DecodeTask task, final CodecPage vuPage) {
        final int viewWidth = task.targetWidth;
        final int pageWidth = vuPage.getWidth();
        final int pageHeight = vuPage.getHeight();
        final RectF nodeBounds = task.pageSliceBounds;
        final float zoom = task.zoom;

        return task.nativeResolution ? getScaledSize(pageWidth, pageWidth, pageHeight, nodeBounds, 1.0f) : getScaledSize(viewWidth, pageWidth, pageHeight, nodeBounds, zoom);
    }

    @Override
    public Rect getScaledSize(final float viewWidth, final float pageWidth, final float pageHeight,
            final RectF nodeBounds, final float zoom) {
        final float scale = 1.0f * viewWidth / pageWidth * zoom;
        final int scaledWidth = Math.round((scale * pageWidth) * nodeBounds.width());
        final int scaledHeight = Math.round((scale * pageHeight) * nodeBounds.height());
        return new Rect(0, 0, scaledWidth, scaledHeight);
    }

    void finishDecoding(final DecodeTask currentDecodeTask, final CodecPage page, final Bitmap bitmap) {
        stopDecoding(currentDecodeTask.node, "complete");
        updateImage(currentDecodeTask, page, bitmap);
    }

    void abortDecoding(final DecodeTask currentDecodeTask, final CodecPage page, final Bitmap bitmap) {
        stopDecoding(currentDecodeTask.node, "failed");
        updateImage(currentDecodeTask, page, bitmap);
    }

    CodecPage getPage(final int pageIndex) {
        final SoftReference<CodecPage> ref = pages.get(pageIndex);
        CodecPage page = ref != null ? ref.get() : null;
        if (page == null) {
            // Cause native recycling last used page if page cache is full now
            // before opening new native page
            pages.put(pageIndex, null);
            page = document.getPage(pageIndex);
            pages.put(pageIndex, new SoftReference<CodecPage>(page));
        }
        return page;
    }

    void updateImage(final DecodeTask currentDecodeTask, final CodecPage page, final Bitmap bitmap) {
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

    class Executor implements RejectedExecutionHandler, Comparator<Runnable> {

        final Map<PageTreeNode, DecodeTask> decodingTasks = new IdentityHashMap<PageTreeNode, DecodeTask>();
        final Map<Long, Future<?>> decodingFutures = new HashMap<Long, Future<?>>();

        final BlockingQueue<Runnable> queue;
        final ThreadPoolExecutor executorService;

        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        Executor() {
            queue = new PriorityBlockingQueue<Runnable>(16, this);
            executorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, queue);
            executorService.setRejectedExecutionHandler(this);
        }

        public void add(final DecodeTask task) {
            lock.writeLock().lock();
            try {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("Adding decoding task: " + task + " for " + task.node);
                }

                final DecodeTask running = decodingTasks.get(task.node);

                if (running != null && running.equals(task) && !isTaskDead(running)) {
                    if (LCTX.isDebugEnabled()) {
                        LCTX.d("The similar task is running: " + running.id + " for " + task.node);
                    }
                    return;
                } else if (running != null) {
                    if (LCTX.isDebugEnabled()) {
                        LCTX.d("The another task is running: " + running.id + " for " + task.node);
                    }
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

        @Override
        public int compare(final Runnable r1, final Runnable r2) {
            final boolean isTask1 = r1 instanceof DecodeTask;
            final boolean isTask2 = r2 instanceof DecodeTask;

            if (isTask1 != isTask2) {
                return isTask1 ? -1 : 1;
            }

            if (!isTask1) {
                return 0;
            }

            final DecodeTask t1 = (DecodeTask) r1;
            final DecodeTask t2 = (DecodeTask) r2;

            return t1.node.getBase().getDocumentController().compare(t1.node, t2.node);
        }

        public void stopDecoding(final DecodeTask task, final PageTreeNode node, final String reason) {
            lock.writeLock().lock();
            try {
                final DecodeTask removed = task == null ? decodingTasks.remove(node) : task;
                final Future<?> future = removed != null ? decodingFutures.remove(removed.id) : null;

                if (removed != null) {
                    if (LCTX.isDebugEnabled()) {
                        LCTX.d("Task " + removed.id + ": Stop decoding task with reason: " + reason + " for "
                                + removed.node);
                    }
                }
                if (future != null) {
                    if (removed == null) {
                        if (LCTX.isDebugEnabled()) {
                            LCTX.d("Stop decoding task for " + node + " with reason: " + reason);
                        }
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

    class DecodeTask implements Runnable {

        final long id = TASK_ID_SEQ.incrementAndGet();
        final PageTreeNode node;
        final int pageNumber;
        final float zoom;
        final DecodeCallback decodeCallback;
        final RectF pageSliceBounds;
        final int targetWidth;
        final boolean nativeResolution;

        DecodeTask(final DecodeCallback decodeCallback, final int targetWidth, final float zoom, final PageTreeNode node, final boolean nativeResolution) {
            this.pageNumber = node.getDocumentPageIndex();
            this.decodeCallback = decodeCallback;
            this.zoom = zoom;
            this.node = node;
            this.pageSliceBounds = node.getPageSliceBounds();
            this.targetWidth = targetWidth;
            this.nativeResolution = nativeResolution;
        }

        @Override
        public void run() {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1);
            performDecode(this);
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
