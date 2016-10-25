package org.ebookdroid.core;

import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.ByteBufferBitmap;
import org.ebookdroid.common.bitmaps.ByteBufferManager;
import org.ebookdroid.common.bitmaps.IBitmapRef;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecFeatures;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageHolder;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.core.crop.PageCropper;
import org.ebookdroid.ui.viewer.IViewController.InvalidateSizeReason;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.utils.CompareUtils;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.MathUtils;

public class DecodeServiceBase implements DecodeService {

    public static final LogContext LCTX = LogManager.root().lctx("Decoding", false);

    static final AtomicLong TASK_ID_SEQ = new AtomicLong();

    final CodecContext codecContext;

    final AtomicBoolean isRecycled;

    final AtomicReference<ViewState> viewState;

    final Map<Integer, CodecPageHolder> pages;

    final Executor executor;

    CodecDocument document;

    public DecodeServiceBase(final CodecContext codecContext) {
        this.codecContext = codecContext;

        isRecycled = new AtomicBoolean();

        viewState = new AtomicReference<ViewState>();

        pages = new PageCache();

        executor = new Executor();

        executor.start();
    }

    @Override
    public boolean isFeatureSupported(final int feature) {
        return codecContext.isFeatureSupported(feature);
    }

    @Override
    @WorkerThread
    public void open(final String fileName, final String password) {
        document = codecContext.openDocument(fileName, password);
    }

    @Override
    public CodecPageInfo getUnifiedPageInfo() {
        return document != null ? document.getUnifiedPageInfo() : null;
    }

    @Override
    public CodecPageInfo getPageInfo(final int pageIndex) {
        return document != null ? document.getPageInfo(pageIndex) : null;
    }

    @Override
    public void updateViewState(final ViewState viewState) {
        this.viewState.set(viewState);
    }

    @Override
    public void searchText(final Page page, final String pattern, final SearchCallback callback) {
        if (isRecycled.get()) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Searching not allowed on recycling");
            }
            return;
        }

        final SearchTask decodeTask = new SearchTask(page, pattern, callback);
        executor.add(decodeTask);
    }

    @Override
    public void stopSearch(final String pattern) {
        executor.stopSearch(pattern);
    }

    @Override
    public void decodePage(final ViewState viewState, final PageTreeNode node) {
        if (isRecycled.get()) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Decoding not allowed on recycling");
            }
            return;
        }

        final DecodeTask decodeTask = new DecodeTask(viewState, node);
        updateViewState(viewState);
        executor.add(decodeTask);
    }

    @Override
    public void stopDecoding(final PageTreeNode node, final String reason) {
        executor.stopDecoding(null, node, reason);
    }

    void performDecode(final DecodeTask task) {
        if (executor.isTaskDead(task)) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d(Thread.currentThread().getName() + ": Task " + task.id + ": Skipping dead decode task for "
                        + task.node);
            }
            return;
        }

        if (LCTX.isDebugEnabled()) {
            LCTX.d(Thread.currentThread().getName() + ": Task " + task.id + ": Starting decoding for " + task.node);
        }

        CodecPageHolder holder = null;
        CodecPage vuPage = null;
        Rect r = null;
        RectF croppedPageBounds = null;

        try {
            holder = getPageHolder(task.id, task.pageNumber);
            vuPage = holder.getPage(task.id);
            if (executor.isTaskDead(task)) {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d(Thread.currentThread().getName() + ": Task " + task.id + ": Abort dead decode task for "
                            + task.node);
                }
                return;
            }

            // Checks if cropping setting is set and node crop region is not set
            if (codecContext.isFeatureSupported(CodecFeatures.FEATURE_CROP_SUPPORT) && task.node.page.shouldCrop() && task.node.getCropping() == null) {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d(Thread.currentThread().getName() + ": Task " + task.id
                            + ": no cropping bounds for task node");
                }
                // Calculate node cropping
                croppedPageBounds = calculateNodeCropping(task, vuPage);
            }

            if (executor.isTaskDead(task)) {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d(Thread.currentThread().getName() + ": Task " + task.id + ": Abort dead decode task for "
                            + task.node);
                }
                return;
            }

            r = getScaledSize(task.node, task.viewState.zoom, croppedPageBounds, vuPage);

            if (LCTX.isDebugEnabled()) {
                LCTX.d(Thread.currentThread().getName() + ": Task " + task.id + ": Rendering rect: " + r);
            }

            final RectF cropping = task.node.page.getCropping(task.node);
            final RectF actualSliceBounds = cropping != null ? cropping : task.node.pageSliceBounds;
            final ByteBufferBitmap bitmap = vuPage.renderBitmap(task.viewState, r.width(), r.height(),
                    actualSliceBounds);

            if (executor.isTaskDead(task)) {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d(Thread.currentThread().getName() + ": Task " + task.id + ": Abort dead decode task for "
                            + task.node);
                }
                ByteBufferManager.release(bitmap);
                return;
            }

            if (task.node.page.links == null) {
                task.node.page.links = vuPage.getPageLinks();
                if (LengthUtils.isNotEmpty(task.node.page.links)) {
                    if (LCTX.isDebugEnabled()) {
                        LCTX.d(Thread.currentThread().getName() + ": Task " + task.id + ": Found links on page "
                                + task.pageNumber + ": " + task.node.page.links);
                    }
                }
            }

            finishDecoding(task, vuPage, bitmap, r, croppedPageBounds);
        } catch (final OutOfMemoryError ex) {
            LCTX.e(Thread.currentThread().getName() + ": Task " + task.id + ": No memory to decode " + task.node);

            for (int i = 0; i <= AppSettings.current().pagesInMemory; i++) {
                pages.put(Integer.MAX_VALUE - i, null);
            }
            pages.clear();
            if (vuPage != null) {
                vuPage.recycle();
            }

            BitmapManager.clear("DecodeService OutOfMemoryError: ");
            ByteBufferManager.clear("DecodeService OutOfMemoryError: ");

            abortDecoding(task, null, null);
        } catch (final Throwable th) {
            LCTX.e(Thread.currentThread().getName() + ": Task " + task.id + ": Decoding failed for " + task.node + ": "
                    + th.getMessage(), th);
            abortDecoding(task, vuPage, null);
        } finally {
            if (holder != null) {
                holder.unlock();
            }
        }
    }

    protected RectF calculateNodeCropping(final DecodeTask task, final CodecPage vuPage) {
        final PageTreeNode root = task.node.page.nodes.root;

        RectF croppedPageBounds = null;

        // Checks if page root node has not been cropped before
        if (root.getCropping() == null) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d(Thread.currentThread().getName() + ": Task " + task.id + ": Decode full page to crop");
            }
            croppedPageBounds = calculateRootCropping(task, root, vuPage);
        }

        if (task.node != root) {
            task.node.evaluateCroppedPageSliceBounds();
        }

        if (LCTX.isDebugEnabled()) {
            LCTX.d(Thread.currentThread().getName() + ": Task " + task.id + ": cropping bounds for task node: "
                    + task.node.getCropping());
        }

        return croppedPageBounds;
    }

    protected RectF calculateRootCropping(final DecodeTask task, final PageTreeNode root, final CodecPage vuPage) {
        final RectF rootBounds = root.pageSliceBounds;
        final ByteBufferBitmap rootBitmap = vuPage.renderBitmap(task.viewState, PageCropper.BMP_SIZE,
                PageCropper.BMP_SIZE, rootBounds);

        final BookSettings bs = task.viewState.book;
        if (bs != null) {
            rootBitmap.applyEffects(bs);
        }

        root.setAutoCropping(PageCropper.getCropBounds(rootBitmap, root.pageSliceBounds), true);

        if (LCTX.isDebugEnabled()) {
            LCTX.d(Thread.currentThread().getName() + ": Task " + task.id + ": cropping root bounds: "
                    + root.getCropping());
        }

        ByteBufferManager.release(rootBitmap);

        final ViewState viewState = task.viewState;
        final PageIndex currentPage = viewState.book.getCurrentPage();
        final float offsetX = viewState.book.offsetX;
        final float offsetY = viewState.book.offsetY;

        viewState.ctrl.invalidatePageSizes(InvalidateSizeReason.PAGE_LOADED, task.node.page);

        final RectF croppedPageBounds = root.page.getBounds(task.viewState.zoom);

        if (LCTX.isDebugEnabled()) {
            LCTX.d(Thread.currentThread().getName() + ": Task " + task.id + ": cropping page bounds: "
                    + croppedPageBounds);
        }

        return croppedPageBounds;
    }

    Rect getScaledSize(final PageTreeNode node, final float zoom, final RectF croppedPageBounds, final CodecPage vuPage) {
        final RectF pageBounds = MathUtils.zoom(croppedPageBounds != null ? croppedPageBounds : node.page.bounds, zoom);
        final RectF r = Page.getTargetRect(node.page.type, pageBounds, node.pageSliceBounds);
        return new Rect(0, 0, (int) r.width(), (int) r.height());
    }

    void finishDecoding(final DecodeTask currentDecodeTask, final CodecPage page, final ByteBufferBitmap bitmap,
            final Rect bitmapBounds, final RectF croppedPageBounds) {
        stopDecoding(currentDecodeTask.node, "complete");
        updateImage(currentDecodeTask, page, bitmap, bitmapBounds, croppedPageBounds);
    }

    void abortDecoding(final DecodeTask currentDecodeTask, final CodecPage page, final ByteBufferBitmap bitmap) {
        stopDecoding(currentDecodeTask.node, "failed");
        updateImage(currentDecodeTask, page, bitmap, null, null);
    }

    CodecPage getPage(final int pageIndex) {
        return getPageHolder(-2, pageIndex).getPage(-2);
    }

    private synchronized CodecPageHolder getPageHolder(final long taskId, final int pageIndex) {
        if (LCTX.isDebugEnabled()) {
            LCTX.d(Thread.currentThread().getName() + "Task " + taskId + ": Codec pages in cache: " + pages.size());
        }
        for (final Iterator<Map.Entry<Integer, CodecPageHolder>> i = pages.entrySet().iterator(); i.hasNext();) {
            final Map.Entry<Integer, CodecPageHolder> entry = i.next();
            final int index = entry.getKey();
            final CodecPageHolder ref = entry.getValue();
            if (ref.isInvalid(-1)) {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d(Thread.currentThread().getName() + "Task " + taskId
                            + ": Remove auto-recycled codec page reference: " + index);
                }
                i.remove();
            }
        }

        CodecPageHolder holder = pages.get(pageIndex);
        if (holder == null) {
            holder = new CodecPageHolder(document, pageIndex);
            pages.put(pageIndex, holder);
        }

        // Preventing problem inside the MuPDF
        if (!codecContext.isFeatureSupported(CodecFeatures.FEATURE_PARALLEL_PAGE_ACCESS)) {
            holder.getPage(taskId);
        }
        return holder;
    }

    void updateImage(final DecodeTask currentDecodeTask, final CodecPage page, final ByteBufferBitmap bitmap,
            final Rect bitmapBounds, final RectF croppedPageBounds) {
        currentDecodeTask.node.decodeComplete(page, bitmap, croppedPageBounds);
    }

    @Override
    public int getPageCount() {
        return document != null ? document.getPageCount() : 0;
    }

    @Override
    public List<OutlineLink> getOutline() {
        return document != null ? document.getOutline() : null;
    }

    @Override
    public void recycle() {
        if (isRecycled.compareAndSet(false, true)) {
            executor.recycle();
        }
    }

    protected int getCacheSize() {
        final ViewState vs = viewState.get();
        int minSize = 1;
        if (vs != null) {
            minSize = vs.pages.lastVisible - vs.pages.firstVisible + 1;
        }
        final int pagesInMemory = AppSettings.current().pagesInMemory;
        return pagesInMemory == 0 ? 0 : Math.max(minSize, pagesInMemory);
    }

    class PageCache extends LinkedHashMap<Integer, CodecPageHolder> {

        private static final long serialVersionUID = -8845124816503128098L;

        @Override
        protected boolean removeEldestEntry(final Map.Entry<Integer, CodecPageHolder> eldest) {
            if (this.size() > getCacheSize()) {
                final CodecPageHolder value = eldest != null ? eldest.getValue() : null;
                if (value != null) {
                    if (value.isInvalid(-1)) {
                        if (LCTX.isDebugEnabled()) {
                            LCTX.d(Thread.currentThread().getName() + ": Remove auto-recycled codec page reference: "
                                    + eldest.getKey());
                        }
                        return true;
                    } else {
                        final boolean recycled = value.recycle(-1, false);
                        if (LCTX.isDebugEnabled()) {
                            if (recycled) {
                                LCTX.d(Thread.currentThread().getName() + ": Recycle and remove old codec page: "
                                        + eldest.getKey());
                            } else {
                                LCTX.d(Thread.currentThread().getName()
                                        + ": Codec page locked and cannot be recycled: " + eldest.getKey());
                            }
                        }
                        return recycled;
                    }
                }
            }
            return false;
        }
    }

    class Executor implements Runnable {

        final Map<PageTreeNode, DecodeTask> decodingTasks = new IdentityHashMap<PageTreeNode, DecodeTask>();

        final ArrayList<Task> tasks;
        final Thread[] threads;
        final ReentrantLock lock = new ReentrantLock();
        final AtomicBoolean run = new AtomicBoolean(true);

        Executor() {
            tasks = new ArrayList<Task>();
            threads = new Thread[AppSettings.current().decodingThreads];

            LCTX.i("Number of decoding threads: " + threads.length);

            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(this, "DecodingThread-" + i);
            }
        }

        void start() {
            final int decodingThreadPriority = AppSettings.current().decodingThreadPriority;
            LCTX.i("Decoding thread priority: " + decodingThreadPriority);

            for (int i = 0; i < threads.length; i++) {
                threads[i].setPriority(decodingThreadPriority);
                threads[i].start();
            }
        }

        @Override
        public void run() {
            try {
                while (run.get()) {
                    final Runnable r = nextTask();
                    if (r != null) {
                        BitmapManager.release();
                        ByteBufferManager.release();
                        r.run();
                    }
                }

            } catch (final Throwable th) {
                LCTX.e(Thread.currentThread().getName() + ": Decoding service executor failed: " + th.getMessage(), th);
                LogManager.onUnexpectedError(th);
            } finally {
                BitmapManager.release();
            }
        }

        Runnable nextTask() {
            final ViewState vs = viewState != null ? viewState.get() : null;
            if (vs == null || vs.app == null || vs.app.decodingOnScroll || vs.ctrl.getView().isScrollFinished()) {
                lock.lock();
                try {
                    if (!tasks.isEmpty()) {
                        return selectBestTask();
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d(Thread.currentThread().getName() + ": view in scrolling");
                }
            }
            synchronized (run) {
                try {
                    run.wait(500);
                } catch (final InterruptedException ex) {
                    Thread.interrupted();
                }
            }
            return null;
        }

        private Runnable selectBestTask() {
            final TaskComparator comp = new TaskComparator(viewState.get());
            Task candidate = null;
            int cindex = 0;

            int index = 0;
            while (index < tasks.size() && candidate == null) {
                candidate = tasks.get(index);
                if (candidate != null && candidate.cancelled.get()) {
                    if (LCTX.isDebugEnabled()) {
                        LCTX.d("---: " + index + "/" + tasks.size() + " " + candidate);
                    }
                    tasks.set(index, null);
                    candidate = null;
                }
                cindex = index;
                index++;
            }
            if (candidate == null) {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d(Thread.currentThread().getName() + ": No tasks in queue");
                }
                tasks.clear();
            } else {
                while (index < tasks.size()) {
                    final Task next = tasks.get(index);
                    if (next != null) {
                        if (next.cancelled.get()) {
                            if (LCTX.isDebugEnabled()) {
                                LCTX.d("---: " + index + "/" + tasks.size() + " " + next);
                            }
                            tasks.set(index, null);
                        } else if (comp.compare(next, candidate) < 0) {
                            candidate = next;
                            cindex = index;
                        }
                    }
                    index++;
                }
                if (LCTX.isDebugEnabled()) {
                    LCTX.d(Thread.currentThread().getName() + ": <<<: " + cindex + "/" + tasks.size() + ": "
                            + candidate);
                }
                tasks.set(cindex, null);
            }
            return candidate;
        }

        public void add(final SearchTask task) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d(Thread.currentThread().getName() + ": Adding search task: " + task + " for " + task.page.index);
            }

            lock.lock();
            try {
                boolean added = false;
                for (int index = 0; index < tasks.size(); index++) {
                    if (null == tasks.get(index)) {
                        tasks.set(index, task);
                        if (LCTX.isDebugEnabled()) {
                            LCTX.d(Thread.currentThread().getName() + ": >>>: " + index + "/" + tasks.size() + ": "
                                    + task);
                        }
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    if (LCTX.isDebugEnabled()) {
                        LCTX.d(Thread.currentThread().getName() + ": +++: " + tasks.size() + "/" + tasks.size() + ": "
                                + task);
                    }
                    tasks.add(task);
                }

                synchronized (run) {
                    run.notifyAll();
                }
            } finally {
                lock.unlock();
            }
        }

        public void stopSearch(final String pattern) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Stop search tasks: " + pattern);
            }

            lock.lock();
            try {
                for (int index = 0; index < tasks.size(); index++) {
                    final Task task = tasks.get(index);
                    if (task instanceof SearchTask) {
                        final SearchTask st = (SearchTask) task;
                        if (st.pattern.equals(pattern)) {
                            tasks.set(index, null);
                        }
                    }
                }
            } finally {
                lock.unlock();
            }
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
                for (int index = 0; index < tasks.size(); index++) {
                    if (null == tasks.get(index)) {
                        tasks.set(index, task);
                        if (LCTX.isDebugEnabled()) {
                            LCTX.d(">>>: " + index + "/" + tasks.size() + ": " + task);
                        }
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    if (LCTX.isDebugEnabled()) {
                        LCTX.d("+++: " + tasks.size() + "/" + tasks.size() + ": " + task);
                    }
                    tasks.add(task);
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
                    if (LCTX.isDebugEnabled()) {
                        LCTX.d(Thread.currentThread().getName() + ": Task " + removed.id
                                + ": Stop decoding task with reason: " + reason + " for " + removed.node);
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

                tasks.add(new ShutdownTask());

                synchronized (run) {
                    run.notifyAll();
                }

            } finally {
                lock.unlock();
            }
        }

        void shutdown() {
            for (final CodecPageHolder ref : pages.values()) {
                ref.recycle(-3, true);
            }
            pages.clear();
            if (document != null) {
                document.recycle();
            }
            codecContext.recycle();
            run.set(false);
        }
    }

    class TaskComparator implements Comparator<Task> {

        final PageTreeNodeComparator cmp;

        public TaskComparator(final ViewState viewState) {
            cmp = viewState != null ? new PageTreeNodeComparator(viewState) : null;
        }

        @Override
        public int compare(final Task r1, final Task r2) {
            if (r1.priority < r2.priority) {
                return -1;
            }
            if (r2.priority < r1.priority) {
                return +1;
            }

            if (r1 instanceof DecodeTask && r2 instanceof DecodeTask) {
                final DecodeTask t1 = (DecodeTask) r1;
                final DecodeTask t2 = (DecodeTask) r2;

                if (cmp != null) {
                    return cmp.compare(t1.node, t2.node);
                }
                return 0;
            }

            return CompareUtils.compare(r1.id, r2.id);
        }

    }

    abstract class Task implements Runnable {

        final long id = TASK_ID_SEQ.incrementAndGet();
        final AtomicBoolean cancelled = new AtomicBoolean();
        final int priority;

        Task(final int priority) {
            this.priority = priority;
        }

    }

    class ShutdownTask extends Task {

        public ShutdownTask() {
            super(0);
        }

        @Override
        public void run() {
            executor.shutdown();
        }
    }

    class SearchTask extends Task {

        final Page page;
        final String pattern;
        final SearchCallback callback;

        public SearchTask(final Page page, final String pattern, final SearchCallback callback) {
            super(1);
            this.page = page;
            this.pattern = pattern;
            this.callback = callback;
        }

        @Override
        public void run() {
            List<? extends RectF> regions = null;
            if (document != null) {
                try {
                    if (codecContext.isFeatureSupported(CodecFeatures.FEATURE_DOCUMENT_TEXT_SEARCH)) {
                        regions = document.searchText(page.index.docIndex, pattern);
                    } else if (codecContext.isFeatureSupported(CodecFeatures.FEATURE_PAGE_TEXT_SEARCH)) {
                        regions = getPage(page.index.docIndex).searchText(pattern);
                    }
                    callback.searchComplete(page, regions);
                } catch (final Throwable th) {
                    LCTX.e("Unexpected error: ", th);
                    callback.searchComplete(page, null);
                }
            }
        }
    }

    class DecodeTask extends Task {

        final long id = TASK_ID_SEQ.incrementAndGet();
        final AtomicBoolean cancelled = new AtomicBoolean();

        final PageTreeNode node;
        final ViewState viewState;
        final int pageNumber;

        DecodeTask(final ViewState viewState, final PageTreeNode node) {
            super(2);
            this.pageNumber = node.page.index.docIndex;
            this.viewState = viewState;
            this.node = node;
        }

        @Override
        public void run() {
            performDecode(this);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof DecodeTask) {
                final DecodeTask that = (DecodeTask) obj;
                return this.pageNumber == that.pageNumber
                        && this.viewState.viewRect.width() == that.viewState.viewRect.width()
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
            buf.append("width").append("=").append((int) viewState.viewRect.width());
            buf.append(", ");
            buf.append("zoom").append("=").append(viewState.zoom);

            buf.append("]");
            return buf.toString();
        }
    }

    @Override
    public IBitmapRef createThumbnail(final boolean useEmbeddedIfAvailable, int width, int height, final int pageNo,
            final RectF region) {
        if (document == null) {
            return null;
        }
        final Bitmap thumbnail = useEmbeddedIfAvailable ? document.getEmbeddedThumbnail() : null;
        if (thumbnail != null) {
            width = 200;
            height = 200;
            final int tw = thumbnail.getWidth();
            final int th = thumbnail.getHeight();
            if (th > tw) {
                width = width * tw / th;
            } else {
                height = height * th / tw;
            }
            final Bitmap scaled = Bitmap.createScaledBitmap(thumbnail, width, height, true);
            final IBitmapRef ref = BitmapManager.addBitmap("Thumbnail", scaled);
            return ref;
        } else {
            final CodecPage page = getPage(pageNo);
            return page.renderBitmap(null, width, height, region).toBitmap();
        }
    }

    @Override
    public ByteBufferBitmap createPageThumbnail(final int width, final int height, final int pageNo, final RectF region) {
        if (document == null) {
            return null;
        }
        final CodecPage page = getPage(pageNo);
        return page.renderBitmap(null, width, height, region);
    }
}
