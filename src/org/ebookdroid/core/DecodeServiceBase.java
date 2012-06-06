package org.ebookdroid.core;

import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.common.log.EmergencyHandler;
import org.ebookdroid.common.log.LogContext;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecDocument.DocSearchNotSupported;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.core.crop.PageCropper;
import org.ebookdroid.ui.viewer.IViewController.InvalidateSizeReason;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;

import java.lang.ref.SoftReference;
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

import org.emdev.utils.CompareUtils;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.MathUtils;

public class DecodeServiceBase implements DecodeService {

    public static final LogContext LCTX = LogContext.ROOT.lctx("Decoding", true);

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
            if (this.size() > getCacheSize()) {
                final SoftReference<CodecPage> value = eldest != null ? eldest.getValue() : null;
                final CodecPage codecPage = value != null ? value.get() : null;
                if (codecPage == null || codecPage.isRecycled()) {
                    if (LCTX.isDebugEnabled()) {
                        LCTX.d("Remove auto-recycled codec page reference: " + eldest.getKey());
                    }
                } else {
                    if (LCTX.isDebugEnabled()) {
                        LCTX.d("Recycle and remove old codec page: " + eldest.getKey());
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
    public int getPixelFormat() {
        final Config cfg = getBitmapConfig();
        switch (cfg) {
            case ALPHA_8:
                return PixelFormat.A_8;
            case ARGB_4444:
                return PixelFormat.RGBA_4444;
            case RGB_565:
                return PixelFormat.RGB_565;
            case ARGB_8888:
                return PixelFormat.RGBA_8888;
            default:
                return PixelFormat.RGB_565;
        }
    }

    @Override
    public Config getBitmapConfig() {
        return this.codecContext.getBitmapConfig();
    }

    @Override
    public void open(final String fileName, final String password) {
        document = codecContext.openDocument(fileName, password);
    }

    @Override
    public CodecPageInfo getUnifiedPageInfo() {
        return document.getUnifiedPageInfo();
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
                LCTX.d("Task " + task.id + ": Skipping dead decode task for " + task.node);
            }
            return;
        }

        if (LCTX.isDebugEnabled()) {
            LCTX.d("Task " + task.id + ": Starting decoding for " + task.node);
        }

        CodecPage vuPage = null;
        Rect r = null;
        RectF croppedPageBounds = null;

        try {
            vuPage = getPage(task.pageNumber);

            if (executor.isTaskDead(task)) {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("Task " + task.id + ": Abort dead decode task for " + task.node);
                }
                return;
            }

            croppedPageBounds = checkCropping(task, vuPage);
            if (executor.isTaskDead(task)) {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("Task " + task.id + ": Abort dead decode task for " + task.node);
                }
                return;
            }

            r = getScaledSize(task.node, task.viewState.zoom, croppedPageBounds, vuPage);

            if (LCTX.isDebugEnabled()) {
                LCTX.d("Task " + task.id + ": Rendering rect: " + r);
            }

            final RectF actualSliceBounds = task.node.croppedBounds != null ? task.node.croppedBounds
                    : task.node.pageSliceBounds;
            final BitmapRef bitmap = vuPage.renderBitmap(r.width(), r.height(), actualSliceBounds);

            if (executor.isTaskDead(task)) {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("Task " + task.id + ": Abort dead decode task for " + task.node);
                }
                BitmapManager.release(bitmap);
                return;
            }

            if (task.node.page.links == null) {
                task.node.page.links = vuPage.getPageLinks();
                if (LengthUtils.isNotEmpty(task.node.page.links)) {
                    if (LCTX.isDebugEnabled()) {
                        LCTX.d("Found links on page " + task.pageNumber + ": " + task.node.page.links);
                    }
                }
            }

            finishDecoding(task, vuPage, bitmap, r, croppedPageBounds);
        } catch (final OutOfMemoryError ex) {
            LCTX.e("Task " + task.id + ": No memory to decode " + task.node);

            for (int i = 0; i <= AppSettings.current().pagesInMemory; i++) {
                pages.put(Integer.MAX_VALUE - i, null);
            }
            pages.clear();
            if (vuPage != null) {
                vuPage.recycle();
            }

            BitmapManager.clear("DecodeService OutOfMemoryError: ");

            abortDecoding(task, null, null);
        } catch (final Throwable th) {
            LCTX.e("Task " + task.id + ": Decoding failed for " + task.node + ": " + th.getMessage(), th);
            abortDecoding(task, vuPage, null);
        }
    }

    RectF checkCropping(final DecodeTask task, final CodecPage vuPage) {
        // Checks if cropping setting is not set
        if (task.viewState.book == null || !task.viewState.book.cropPages) {
            return null;
        }
        // Checks if page has been cropped before
        if (task.node.croppedBounds != null) {
            // Page size is actuall now
            return null;
        }

        if (LCTX.isDebugEnabled()) {
            LCTX.d("Task " + task.id + ": no cropping bounds for task node");
        }

        RectF croppedPageBounds = null;

        // Checks if page root node has been cropped before
        final PageTreeNode root = task.node.page.nodes.root;
        if (root.croppedBounds == null) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Task " + task.id + ": Decode full page to crop");
            }
            final Rect rootRect = new Rect(0, 0, PageCropper.BMP_SIZE, PageCropper.BMP_SIZE);
            final RectF rootBounds = root.pageSliceBounds;

            final BitmapRef rootBitmap = vuPage.renderBitmap(rootRect.width(), rootRect.height(), rootBounds);
            root.croppedBounds = PageCropper.getCropBounds(rootBitmap, rootRect, root.pageSliceBounds);

            if (LCTX.isDebugEnabled()) {
                LCTX.d("Task " + task.id + ": cropping root bounds: " + root.croppedBounds);
            }

            BitmapManager.release(rootBitmap);

            final ViewState viewState = task.viewState;
            final float pageWidth = vuPage.getWidth() * root.croppedBounds.width();
            final float pageHeight = vuPage.getHeight() * root.croppedBounds.height();

            final PageIndex currentPage = viewState.book.getCurrentPage();
            final float offsetX = viewState.book.offsetX;
            final float offsetY = viewState.book.offsetY;

            root.page.setAspectRatio(pageWidth, pageHeight);
            viewState.ctrl.invalidatePageSizes(InvalidateSizeReason.PAGE_LOADED, task.node.page);

            croppedPageBounds = root.page.getBounds(task.viewState.zoom);

            if (LCTX.isDebugEnabled()) {
                LCTX.d("Task " + task.id + ": cropping page bounds: " + croppedPageBounds);
            }

            task.node.page.base.getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    viewState.ctrl.goToPage(currentPage.viewIndex, offsetX, offsetY);
                }
            });

        }

        if (task.node != root) {
            task.node.croppedBounds = PageTreeNode.evaluateCroppedPageSliceBounds(task.node.pageSliceBounds,
                    task.node.parent);
        }

        if (LCTX.isDebugEnabled()) {
            LCTX.d("Task " + task.id + ": cropping bounds for task node: " + task.node.croppedBounds);
        }

        return croppedPageBounds;
    }

    Rect getScaledSize(final PageTreeNode node, final float zoom, final RectF croppedPageBounds, final CodecPage vuPage) {
        final RectF pageBounds = MathUtils.zoom(croppedPageBounds != null ? croppedPageBounds : node.page.bounds, zoom);
        final RectF r = Page.getTargetRect(node.page.type, pageBounds, node.pageSliceBounds);
        return new Rect(0, 0, (int) r.width(), (int) r.height());
    }

    void finishDecoding(final DecodeTask currentDecodeTask, final CodecPage page, final BitmapRef bitmap,
            final Rect bitmapBounds, final RectF croppedPageBounds) {
        stopDecoding(currentDecodeTask.node, "complete");
        updateImage(currentDecodeTask, page, bitmap, bitmapBounds, croppedPageBounds);
    }

    void abortDecoding(final DecodeTask currentDecodeTask, final CodecPage page, final BitmapRef bitmap) {
        stopDecoding(currentDecodeTask.node, "failed");
        updateImage(currentDecodeTask, page, bitmap, null, null);
    }

    CodecPage getPage(final int pageIndex) {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("Codec pages in cache: " + pages.size());
        }
        for (final Iterator<Map.Entry<Integer, SoftReference<CodecPage>>> i = pages.entrySet().iterator(); i.hasNext();) {
            final Map.Entry<Integer, SoftReference<CodecPage>> entry = i.next();
            final int index = entry.getKey();
            final SoftReference<CodecPage> ref = entry.getValue();
            final CodecPage page = ref != null ? ref.get() : null;
            if (page == null || page.isRecycled()) {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("Remove auto-recycled codec page reference: " + index);
                }
                i.remove();
            }
        }

        final SoftReference<CodecPage> ref = pages.get(pageIndex);
        CodecPage page = ref != null ? ref.get() : null;
        if (page == null || page.isRecycled()) {
            // Cause native recycling last used page if page cache is full now
            // before opening new native page
            pages.put(pageIndex, null);
            page = document.getPage(pageIndex);
        }
        pages.put(pageIndex, new SoftReference<CodecPage>(page));
        return page;
    }

    void updateImage(final DecodeTask currentDecodeTask, final CodecPage page, final BitmapRef bitmap,
            final Rect bitmapBounds, final RectF croppedPageBounds) {
        currentDecodeTask.node.decodeComplete(page, bitmap, bitmapBounds, croppedPageBounds);
    }

    @Override
    public int getPageCount() {
        System.out.println("DecodeServiceBase.getPageCount(" + this.hashCode() + "): " + document);
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

    protected int getCacheSize() {
        final ViewState vs = viewState.get();
        int minSize = 1;
        if (vs != null) {
            minSize = vs.pages.lastVisible - vs.pages.firstVisible + 1;
        }
        int pagesInMemory = AppSettings.current().pagesInMemory;
        return pagesInMemory == 0 ? 0 : Math.max(minSize, pagesInMemory);
    }

    class Executor implements Runnable {

        final Map<PageTreeNode, DecodeTask> decodingTasks = new IdentityHashMap<PageTreeNode, DecodeTask>();

        final ArrayList<Task> tasks;
        final Thread thread;
        final ReentrantLock lock = new ReentrantLock();
        final AtomicBoolean run = new AtomicBoolean(true);

        Executor() {
            tasks = new ArrayList<Task>();
            thread = new Thread(this);

            final int decodingThreadPriority = AppSettings.current().decodingThreadPriority;
            LCTX.i("Decoding thread priority: " + decodingThreadPriority);
            thread.setPriority(decodingThreadPriority);

            thread.start();
        }

        @Override
        public void run() {
            try {
                while (run.get()) {
                    final Runnable r = nextTask();
                    if (r != null) {
                        BitmapManager.release();
                        r.run();
                    }
                }

            } catch (final Throwable th) {
                LCTX.e("Decoding service executor failed: " + th.getMessage(), th);
                EmergencyHandler.onUnexpectedError(th);
            } finally {
                BitmapManager.release();
            }
        }

        Runnable nextTask() {
            lock.lock();
            try {
                if (!tasks.isEmpty()) {
                    final TaskComparator comp = new TaskComparator(viewState.get());
                    Task candidate = null;
                    int cindex = 0;

                    int index = 0;
                    while (index < tasks.size() && candidate == null) {
                        candidate = tasks.get(index);
                        cindex = index;
                        index++;
                    }
                    if (candidate == null) {
                        if (LCTX.isDebugEnabled()) {
                            LCTX.d("No tasks in queue");
                        }
                        tasks.clear();
                    } else {
                        while (index < tasks.size()) {
                            final Task next = tasks.get(index);
                            if (next != null && comp.compare(next, candidate) < 0) {
                                candidate = next;
                                cindex = index;
                            }
                            index++;
                        }
                        if (LCTX.isDebugEnabled()) {
                            LCTX.d("<<<: " + cindex + "/" + tasks.size() + ": " + candidate);
                        }
                        tasks.set(cindex, null);
                    }
                    return candidate;
                }
            } finally {
                lock.unlock();
            }
            synchronized (run) {
                try {
                    run.wait(60000);
                } catch (final InterruptedException ex) {
                    Thread.interrupted();
                }
            }
            return null;
        }

        public void add(final SearchTask task) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Adding search task: " + task + " for " + task.page.index);
            }

            lock.lock();
            try {
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
                    for (int i = 0; i < tasks.size(); i++) {
                        if (removed == tasks.get(i)) {
                            if (LCTX.isDebugEnabled()) {
                                LCTX.d("---: " + i + "/" + tasks.size() + " " + removed);
                            }
                            tasks.set(i, null);
                            break;
                        }
                    }
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

                tasks.add(new ShutdownTask());

                synchronized (run) {
                    run.notifyAll();
                }

            } finally {
                lock.unlock();
            }
        }

        void shutdown() {
            for (final SoftReference<CodecPage> ref : pages.values()) {
                final CodecPage page = ref != null ? ref.get() : null;
                if (page != null) {
                    page.recycle();
                }
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
            try {
                try {
                    regions = document.searchText(page.index.docIndex, pattern);
                } catch (final DocSearchNotSupported ex) {
                    regions = getPage(page.index.docIndex).searchText(pattern);
                }
                callback.searchComplete(page, regions);
            } catch (final Throwable th) {
                LCTX.e("Unexpected error: ", th);
                callback.searchComplete(page, null);
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
    public BitmapRef createThumbnail(int width, int height, final int pageNo, final RectF region) {
        if (document == null) {
            return null;
        }
        final Bitmap thumbnail = document.getEmbeddedThumbnail();
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
            final BitmapRef ref = BitmapManager.addBitmap("Thumbnail", scaled);
            return ref;
        } else {
            final CodecPage page = getPage(pageNo);
            return page.renderBitmap(width, height, region);
        }
    }

    @Override
    public boolean isPageSizeCacheable() {
        return codecContext.isPageSizeCacheable();
    }
}
