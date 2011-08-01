package org.ebookdroid.core;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Log;

import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.utils.PathFromUri;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DecodeServiceBase implements DecodeService
{
    public static final String DECODE_SERVICE = "ViewDroidDecodeService";

	private static final int PAGE_POOL_SIZE = 16;
    private static final AtomicLong TASK_ID_SEQ = new AtomicLong();

    private final CodecContext codecContext;

    private final Executor executor = new Executor();

    private CodecDocument document;
    private final HashMap<Integer, SoftReference<CodecPage>> pages = new HashMap<Integer, SoftReference<CodecPage>>();
    private ContentResolver contentResolver;
    private Queue<Integer> pageEvictionQueue = new LinkedList<Integer>();
    private AtomicBoolean isRecycled = new AtomicBoolean();

    public DecodeServiceBase(CodecContext codecContext)
    {
        this.codecContext = codecContext;
    }

    @Override
    public void setContentResolver(ContentResolver contentResolver)
    {
        this.contentResolver = contentResolver;
        codecContext.setContentResolver(contentResolver);
    }

    @Override
    public void open(Uri fileUri, String password)
    {
        document = codecContext.openDocument(PathFromUri.retrieve(contentResolver, fileUri), password);
    }

    public void decodePage(PageTreeNode node, int targetWidth, float zoom, final DecodeCallback decodeCallback)
    {
        final DecodeTask decodeTask = new DecodeTask(decodeCallback, targetWidth, zoom, node);

        if (isRecycled.get()) {
            Log.d(DECODE_SERVICE, "Decoding not allowed on recycling");
            return;
        }

        executor.add(decodeTask);
    }

    @Override
    public void stopDecoding(PageTreeNode node, String reason)
    {
        executor.stopDecoding(null, node, reason);
    }

    private void performDecode(DecodeTask task)
            throws IOException
    {
        if (executor.isTaskDead(task))
        {
            Log.d(DECODE_SERVICE, "Task "+ task.id + ": Skipping dead decode task");
            return;
        }

        Log.d(DECODE_SERVICE, "Task "+ task.id + ": Starting decoding");

        CodecPage vuPage = getPage(task.pageNumber);
//        preloadNextPage(task.pageNumber);

        if (executor.isTaskDead(task))
        {
            Log.d(DECODE_SERVICE, "Task "+ task.id + ": Abort dead decode task");
            return;
        }

        Log.d(DECODE_SERVICE, "Task "+ task.id + ": Start converting map to bitmap");
        float scale = calculateScale(vuPage, task.targetWidth) * task.zoom;
        final Bitmap bitmap = vuPage.renderBitmap(getScaledWidth(task, vuPage, scale), getScaledHeight(task, vuPage, scale), task.pageSliceBounds);
        Log.d(DECODE_SERVICE, "Task "+ task.id + ": Converting map to bitmap finished");

        if (executor.isTaskDead(task))
        {
            Log.d(DECODE_SERVICE, "Task "+ task.id + ": Abort dead decode task");
            bitmap.recycle();
            return;
        }

        Log.d(DECODE_SERVICE, "Task "+ task.id + ": Finish decoding task");
        finishDecoding(task, bitmap);
    }

    private int getScaledHeight(DecodeTask currentDecodeTask, CodecPage vuPage, float scale)
    {
        return Math.round(getScaledHeight(vuPage, scale) * currentDecodeTask.pageSliceBounds.height());
    }

    private int getScaledWidth(DecodeTask currentDecodeTask, CodecPage vuPage, float scale)
    {
        return Math.round(getScaledWidth(vuPage, scale) * currentDecodeTask.pageSliceBounds.width());
    }

    private int getScaledHeight(CodecPage vuPage, float scale)
    {
        return (int) (scale * vuPage.getHeight());
    }

    private int getScaledWidth(CodecPage vuPage, float scale)
    {
        return (int) (scale * vuPage.getWidth());
    }

    private float calculateScale(CodecPage codecPage, int targetWidth)
    {
        return 1.0f * targetWidth / codecPage.getWidth();
    }

    private void finishDecoding(DecodeTask currentDecodeTask, Bitmap bitmap)
    {
        stopDecoding(currentDecodeTask.node, "complete");
        updateImage(currentDecodeTask, bitmap);
    }

    private void preloadNextPage(int pageNumber) throws IOException
    {
        final int nextPage = pageNumber + 1;
        if (nextPage >= getPageCount())
        {
            return;
        }
        getPage(nextPage);
    }

    private CodecPage getPage(int pageIndex)
    {
        if (!pages.containsKey(pageIndex) || pages.get(pageIndex).get() == null)
        {
            pages.put(pageIndex, new SoftReference<CodecPage>(document.getPage(pageIndex)));
            pageEvictionQueue.remove(pageIndex);
            pageEvictionQueue.offer(pageIndex);
            if (pageEvictionQueue.size() > PAGE_POOL_SIZE) {
            	Integer evictedPageIndex = pageEvictionQueue.poll();
            	CodecPage evictedPage = pages.remove(evictedPageIndex).get();
            	if (evictedPage != null) {
            		evictedPage.recycle();
            	}
            }
        }
        return pages.get(pageIndex).get();
    }

    @Override
    public int getEffectivePagesWidth(int targetWidth)
    {
        final CodecPage page = getPage(0);
        return getScaledWidth(page, calculateScale(page, targetWidth));
    }

    @Override
    public int getEffectivePagesHeight(int targetWidth)
    {
        final CodecPage page = getPage(0);
        return getScaledHeight(page, calculateScale(page, targetWidth));
    }

    @Override
    public int getPageWidth(int pageIndex)
    {
        return getPage(pageIndex).getWidth();
    }

    @Override
    public int getPageHeight(int pageIndex)
    {
        return getPage(pageIndex).getHeight();
    }

    private void updateImage(final DecodeTask currentDecodeTask, Bitmap bitmap)
    {
        currentDecodeTask.decodeCallback.decodeComplete(bitmap);
    }

    @Override
    public int getPageCount()
    {
        return document.getPageCount();
    }

    @Override
    public List<OutlineLink> getOutline()
    {
    	return document.getOutline();
    }

    @Override
    public void recycle()
    {
        if (isRecycled.compareAndSet(false, true))
        {
            executor.recycle();
        }
    }

    private class Executor implements RejectedExecutionHandler, Comparator<Runnable>
    {
        private final Map<PageTreeNode, DecodeTask> decodingTasks = new HashMap<PageTreeNode, DecodeTask>();
        private final Map<Long, Future<?>> decodingFutures = new HashMap<Long, Future<?>>();

        private final BlockingQueue<Runnable> queue;
        private final ThreadPoolExecutor executorService;

        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        public Executor() {
            queue = new LinkedBlockingQueue<Runnable>(); //new PriorityBlockingQueue<Runnable>(16, this);
            executorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, queue);
            executorService.setRejectedExecutionHandler(this);
        }

        public void add(DecodeTask task)
        {
            lock.writeLock().lock();
            try
            {
                Log.d(DECODE_SERVICE, "Adding decoding task: " + task);

                DecodeTask running = decodingTasks.get(task.node);
                if (running != null && running.equals(task) && !isTaskDead(running)) {
                    Log.d(DECODE_SERVICE, "The similar task is running: " + running.id);
                    return;
                }

                final Future<?> future = executorService.submit(task);

                decodingFutures.put(task.id, future);
                final DecodeTask old = decodingTasks.put(task.node, task);
                if (old != null)
                {
                    stopDecoding(old, null, "canceled by new one");
                }
            } finally
            {
                lock.writeLock().unlock();
            }
        }

        public void stopDecoding(DecodeTask task, PageTreeNode node, String reason)
        {
            lock.writeLock().lock();
            try
            {
                final DecodeTask removed = task == null ? decodingTasks.remove(node) : task;
                final Future<?> future = removed != null ? decodingFutures.remove(removed.id) : null;

                if (removed != null) {
                    Log.d(DECODE_SERVICE, "Stop decoding task: " + removed.id + " with reason: " + reason);
                }
                if (future != null)
                {
                    if (removed == null) {
                        Log.d(DECODE_SERVICE, "Stop decoding task for " + node + " with reason: " + reason);
                    }
                    future.cancel(false);
                }
            } finally
            {
                lock.writeLock().unlock();
            }
        }

        public boolean isTaskDead(DecodeTask task)
        {
            lock.readLock().lock();
            try
            {
                Future<?> future = decodingFutures.get(task.id);
                if (future != null)
                {
                    return future.isCancelled();
                }
                return true;
            } finally
            {
                lock.readLock().unlock();
            }
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor)
        {
            if (r instanceof DecodeTask) {
                stopDecoding(((DecodeTask) r), null, "rejected by executor");
            }
        }

        public void recycle()
        {
            lock.writeLock().lock();
            try
            {
                for (DecodeTask task : decodingTasks.values())
                {
                    stopDecoding(task, null, "recycling");
                }
                executorService.submit(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        for (SoftReference<CodecPage> codecPageSoftReference : pages.values())
                        {
                            CodecPage page = codecPageSoftReference.get();
                            if (page != null)
                            {
                                page.recycle();
                            }
                        }
                        document.recycle();
                        codecContext.recycle();
                        executorService.shutdown();
                    }
                });
            } finally
            {
                lock.writeLock().unlock();
            }
        }

        @Override
        public int compare(Runnable r1, Runnable r2)
        {
            boolean isTask1 = r1 instanceof DecodeTask;
            boolean isTask2 = r2 instanceof DecodeTask;

            if (isTask1 != isTask2) {
                return isTask1 ? -1 : 1;
            }

            if (!isTask1) {
                return 0;
            }

            DecodeTask t1 = (DecodeTask) r1;
            DecodeTask t2 = (DecodeTask) r2;

            int currentPageIndex = t1.node.getBase().getDocumentModel().getCurrentPageIndex();

            int d1 = Math.abs(t1.node.getPageIndex() - currentPageIndex);
            int d2 = Math.abs(t2.node.getPageIndex() - currentPageIndex);
            return d1 < d2 ? -1 : d1 > d2 ? +1 : 0;
        }
    }

    private class DecodeTask implements Runnable
    {
        private final long id = TASK_ID_SEQ.incrementAndGet();
        private final PageTreeNode node;
        private final int pageNumber;
        private final float zoom;
        private final DecodeCallback decodeCallback;
        private final RectF pageSliceBounds;
        private final int targetWidth;

        private DecodeTask(DecodeCallback decodeCallback, int targetWidth, float zoom, PageTreeNode node)
        {
            this.pageNumber = node.getDocumentPageIndex();
            this.decodeCallback = decodeCallback;
            this.zoom = zoom;
            this.node = node;
            this.pageSliceBounds = node.getPageSliceBounds();
            this.targetWidth = targetWidth;
        }

        @Override
        public void run()
        {
            try
            {
                Thread.currentThread().setPriority(Thread.NORM_PRIORITY-1);
                performDecode(this);
            }
            catch (IOException e)
            {
                Log.e(DECODE_SERVICE, "Decode fail", e);
            }
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) {
                return true;
            }
            if (obj instanceof DecodeTask) {
                DecodeTask that = (DecodeTask) obj;
                return this.pageNumber == that.pageNumber && this.pageSliceBounds.equals(that.pageSliceBounds) && this.targetWidth == that.targetWidth
                        && this.zoom == that.zoom;
            }
            return false;
        }

        @Override
        public String toString() {
          StringBuilder buf = new StringBuilder("DecodeTask");
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
	public CodecPageInfo getPageInfo(int pageIndex) {
		return document.getPageInfo(pageIndex, codecContext);
	}

}
