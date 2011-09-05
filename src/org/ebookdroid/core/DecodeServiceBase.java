package org.ebookdroid.core;

import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.log.EmergencyHandler;
import org.ebookdroid.core.log.LogContext;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class DecodeServiceBase implements DecodeService {

    public static final LogContext LCTX = LogContext.ROOT.lctx("Decoding");

    static final int PAGE_POOL_SIZE = 16;

    static final AtomicLong TASK_ID_SEQ = new AtomicLong();

    final CodecContext codecContext;

    final Executor executor = new Executor();

    final AtomicBoolean isRecycled = new AtomicBoolean();

    final AtomicReference<ViewState> viewState = new AtomicReference<ViewState>();

    CodecDocument document;

    final Map<Integer, SoftReference<CodecPage>> pages = new LinkedHashMap<Integer, SoftReference<CodecPage>>() {

        private static final long serialVersionUID = -8845124816503128098L;

        @Override
        protected boolean removeEldestEntry(final Map.Entry<Integer, SoftReference<CodecPage>> eldest) {
            if (this.size() > PAGE_POOL_SIZE) {
                final SoftReference<CodecPage> value = eldest != null ? eldest.getValue() : null;
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
    public CodecPageInfo getPageInfo(final int pageIndex) {
        return document.getPageInfo(pageIndex);
    }

    @Override
    public void updateViewState(final ViewState viewState) {
        this.viewState.set(viewState);
    }

    @Override
    public void decodePage(final ViewState viewState, final PageTreeNode node) {
        final DecodeTask decodeTask = new DecodeTask(viewState, node);
        updateViewState(viewState);

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
            final Bitmap bitmap = vuPage.renderBitmap(r.width(), r.height(), task.pageSliceBounds);

            if (executor.isTaskDead(task)) {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("Task " + task.id + ": Abort dead decode task for " + task.node);
                }
                bitmap.recycle();
                return;
            }

            finishDecoding(task, vuPage, bitmap);
        } catch (final OutOfMemoryError ex) {
            LCTX.e("Task " + task.id + ": No memory to decode " + task.node);
            for (int i = 0; i < PAGE_POOL_SIZE; i++) {
                pages.put(Integer.MAX_VALUE - i, null);
            }
            vuPage.recycle();
            abortDecoding(task, null, null);
        } catch (final Throwable th) {
            LCTX.e("Task " + task.id + ": Decoding failed for " + task.node + ": " + th.getMessage(), th);
            abortDecoding(task, vuPage, null);
        }
    }

    Rect getScaledSize(final DecodeTask task, final CodecPage vuPage) {
        final int viewWidth = (int) task.viewState.realRect.width();
        final int pageWidth = vuPage.getWidth();
        final int pageHeight = vuPage.getHeight();
        final RectF nodeBounds = task.pageSliceBounds;
        final float zoom = task.viewState.zoom;

        if (task.viewState.decodeMode == DecodeMode.NATIVE_RESOLUTION) {
            return getNativeSize(pageWidth, pageHeight, nodeBounds, task.node.page.getTargetRectScale());
        }
        return getScaledSize(viewWidth, pageWidth, pageHeight, nodeBounds, zoom, task.node.page.getTargetRectScale());
    }

    @Override
    public Rect getNativeSize(final float pageWidth, final float pageHeight, final RectF nodeBounds,
            final float pageTypeWidthScale) {

        final int scaledWidth = Math.round((pageWidth * pageTypeWidthScale) * nodeBounds.width());
        final int scaledHeight = Math.round((pageHeight * pageTypeWidthScale) * nodeBounds.height());
        return new Rect(0, 0, scaledWidth, scaledHeight);
    }

    @Override
    public Rect getScaledSize(final float viewWidth, final float pageWidth, final float pageHeight,
            final RectF nodeBounds, final float zoom, final float pageTypeWidthScale) {
        final float scale = 1.0f * viewWidth / pageWidth * zoom;
        final int scaledWidth = Math.round((scale * pageWidth * pageTypeWidthScale) * nodeBounds.width());
        final int scaledHeight = Math.round((scale * pageHeight * pageTypeWidthScale) * nodeBounds.height());
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
        currentDecodeTask.node.decodeComplete(page, bitmap);
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

    class Executor implements Runnable {

        final Map<PageTreeNode, DecodeTask> decodingTasks = new IdentityHashMap<PageTreeNode, DecodeTask>();

        final ArrayList<Runnable> queue;
        final Thread thread;
        final ReentrantLock lock = new ReentrantLock();
        final AtomicBoolean run = new AtomicBoolean(true);

        Executor() {
            queue = new ArrayList<Runnable>();
            thread = new Thread(this);
            thread.start();
        }

        @Override
        public void run() {
            try {
                while (run.get()) {
                    final Runnable r = nextTask();
                    if (r != null) {
                        r.run();
                    }
                }

            } catch (final Throwable th) {
                LCTX.e("Decoding service executor failed: " + th.getMessage(), th);
                EmergencyHandler.onUnexpectedError(th);
            }
        }

        Runnable nextTask() {
            lock.lock();
            try {
                if (!queue.isEmpty()) {
                    final TaskComparator comp = new TaskComparator(viewState.get());
                    Runnable candidate = null;
                    int cindex = 0;

                    int index = 0;
                    while (index < queue.size() && candidate == null) {
                        candidate = queue.get(index);
                        cindex = index;
                        index++;
                    }
                    if (candidate == null) {
                        queue.clear();
                    } else {
                        while (index < queue.size()) {
                            final Runnable next = queue.get(index);
                            if (next != null && comp.compare(next, candidate) < 0) {
                                candidate = next;
                                cindex = index;
                            }
                            index++;
                        }
                        queue.set(cindex, null);
                    }
                    return candidate;
                }
            } finally {
                lock.unlock();
            }
            synchronized (run) {
                try {
                    run.wait(1000);
                } catch (final InterruptedException ex) {
                    Thread.interrupted();
                }
            }
            return null;
        }

        public void add(final DecodeTask task) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Adding decoding task: " + task + " for " + task.node);
            }

            lock.lock();
            try {
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
                boolean added = false;
                for (int index = 0; index < queue.size() && !added; index++) {
                    if (null == queue.get(index)) {
                        queue.set(index, task);
                        added = true;
                    }
                }
                if (!added) {
                    queue.add(task);
                }

                synchronized (run) {
                    run.notifyAll();
                }

                if (running != null) {
                    stopDecoding(running, null, "canceled by new one");
                }
            } finally {
                lock.unlock();
            }
        }

        public void stopDecoding(final DecodeTask task, final PageTreeNode node, final String reason) {
            lock.lock();
            try {
                final DecodeTask removed = task == null ? decodingTasks.remove(node) : task;

                if (removed != null) {
                    removed.cancelled.set(true);
                    queue.remove(removed);
                    if (LCTX.isDebugEnabled()) {
                        LCTX.d("Task " + removed.id + ": Stop decoding task with reason: " + reason + " for "
                                + removed.node);
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        public boolean isTaskDead(final DecodeTask task) {
            return task.cancelled.get();
        }

        public void recycle() {
            lock.lock();
            try {
                for (final DecodeTask task : decodingTasks.values()) {
                    stopDecoding(task, null, "recycling");
                }

                queue.add(new Runnable() {

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
                        run.set(false);
                    }
                });

                synchronized (run) {
                    run.notifyAll();
                }

            } finally {
                lock.unlock();
            }
        }
    }

    class TaskComparator implements Comparator<Runnable> {

        final PageTreeNodeComparator cmp;

        public TaskComparator(final ViewState viewState) {
            cmp = viewState != null ? new PageTreeNodeComparator(viewState) : null;
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

            if (cmp != null) {
                return cmp.compare(t1.node, t2.node);
            }

            return 0;
        }

    }

    class DecodeTask implements Runnable {

        final long id = TASK_ID_SEQ.incrementAndGet();
        final AtomicBoolean cancelled = new AtomicBoolean();

        final PageTreeNode node;
        final ViewState viewState;
        final int pageNumber;
        final RectF pageSliceBounds;

        DecodeTask(final ViewState viewState, final PageTreeNode node) {
            this.pageNumber = node.getDocumentPageIndex();
            this.viewState = viewState;
            this.node = node;
            this.pageSliceBounds = node.getPageSliceBounds();
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
                        && this.viewState.realRect.width() == that.viewState.realRect.width()
                        && this.viewState.zoom == that.viewState.zoom;
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
            buf.append("width").append("=").append((int) viewState.realRect.width());
            buf.append(", ");
            buf.append("zoom").append("=").append(viewState.zoom);
            buf.append(", ");
            buf.append("bounds").append("=").append(pageSliceBounds);

            buf.append("]");
            return buf.toString();
        }
    }

    @Override
    public void createThumbnail(File thumbnailFile, int width, int height) {
        CodecPage page = getPage(0);
        Bitmap bmp = page.renderBitmap(width, height, new RectF(0, 0, 1, 1));
        
        FileOutputStream out;
        try {
            out = new FileOutputStream(thumbnailFile);
            bmp.compress(Bitmap.CompressFormat.JPEG, 50, out);
            out.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

}
