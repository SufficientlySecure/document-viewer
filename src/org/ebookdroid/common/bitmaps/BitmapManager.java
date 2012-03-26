package org.ebookdroid.common.bitmaps;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.common.log.LogContext;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Debug;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.emdev.utils.LengthUtils;

public class BitmapManager {

    static final LogContext LCTX = LogContext.ROOT.lctx("BitmapManager", false);

    private final static long BITMAP_MEMORY_LIMIT = Runtime.getRuntime().maxMemory() / 2;

    private static final int GENERATION_THRESHOLD = 10;

    private static Map<Integer, BitmapRef> used = new ConcurrentHashMap<Integer, BitmapRef>();

    private static Queue<BitmapRef> pool = new ConcurrentLinkedQueue<BitmapRef>();

    private static SparseArray<Bitmap> resources = new SparseArray<Bitmap>();

    private static Queue<Object> releasing = new ConcurrentLinkedQueue<Object>();

    private static final AtomicLong created = new AtomicLong();
    private static final AtomicLong reused = new AtomicLong();

    private static final AtomicLong memoryUsed = new AtomicLong();
    private static final AtomicLong memoryPooled = new AtomicLong();

    private static AtomicLong generation = new AtomicLong();

    static int partSize = 1 << 7;

    static boolean useEarlyRecycling = false;

    static boolean useBitmapHack = false;

    public static Bitmap getResource(final int resourceId) {
        synchronized (resources) {
            Bitmap bitmap = resources.get(resourceId);
            if (bitmap == null || bitmap.isRecycled()) {
                final Resources resources = EBookDroidApp.context.getResources();
                bitmap = BitmapFactory.decodeResource(resources, resourceId);
            }
            return bitmap;
        }
    }

    public static BitmapRef addBitmap(final String name, final Bitmap bitmap) {
        final BitmapRef ref = new BitmapRef(bitmap, generation.get());
        used.put(ref.id, ref);

        created.incrementAndGet();
        memoryUsed.addAndGet(ref.size);

        if (LCTX.isDebugEnabled()) {
            LCTX.d("Added bitmap: [" + ref.id + ", " + name + ", " + ref.width + ", " + ref.height + "], created="
                    + created + ", reused=" + reused + ", memoryUsed=" + used.size() + "/" + (memoryUsed.get() / 1024)
                    + "KB" + ", memoryInPool=" + pool.size() + "/" + (memoryPooled.get() / 1024) + "KB");
        }

        ref.name = name;
        return ref;
    }

    public static BitmapRef getBitmap(final String name, final int width, final int height, final Bitmap.Config config) {
        if (used.isEmpty() && pool.isEmpty()) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("!!! Bitmap pool size: " + (BITMAP_MEMORY_LIMIT / 1024) + "KB");
            }
        }

        final Iterator<BitmapRef> it = pool.iterator();
        while (it.hasNext()) {
            final BitmapRef ref = it.next();
            final Bitmap bmp = ref.bitmap;

            if (bmp != null && bmp.getConfig() == config && ref.width == width && ref.height >= height) {
                it.remove();

                ref.gen = generation.get();
                used.put(ref.id, ref);

                reused.incrementAndGet();
                memoryPooled.addAndGet(-ref.size);
                memoryUsed.addAndGet(ref.size);

                if (LCTX.isDebugEnabled()) {
                    LCTX.d("Reuse bitmap: [" + ref.id + ", " + ref.name + " => " + name + ", " + width + ", " + height
                            + "], created=" + created + ", reused=" + reused + ", memoryUsed=" + used.size() + "/"
                            + (memoryUsed.get() / 1024) + "KB" + ", memoryInPool=" + pool.size() + "/"
                            + (memoryPooled.get() / 1024) + "KB");
                }
                bmp.eraseColor(Color.CYAN);
                ref.name = name;
                return ref;
            }
        }

        final BitmapRef ref = new BitmapRef(Bitmap.createBitmap(width, height, config), generation.get());
        used.put(ref.id, ref);

        created.incrementAndGet();
        memoryUsed.addAndGet(ref.size);

        if (LCTX.isDebugEnabled()) {
            LCTX.d("Create bitmap: [" + ref.id + ", " + name + ", " + width + ", " + height + "], created=" + created
                    + ", reused=" + reused + ", memoryUsed=" + used.size() + "/" + (memoryUsed.get() / 1024) + "KB"
                    + ", memoryInPool=" + pool.size() + "/" + (memoryPooled.get() / 1024) + "KB");
        }

        shrinkPool(BITMAP_MEMORY_LIMIT);

        ref.name = name;
        return ref;
    }

    public static void clear(final String msg) {
        generation.addAndGet(GENERATION_THRESHOLD * 2);
        removeOldRefs();
        release();
        shrinkPool(0);
        print(msg, true);
    }

    private static void print(final String msg, final boolean showRefs) {
        long sum = 0;
        for (final BitmapRef ref : pool) {
            if (!ref.isRecycled()) {
                if (showRefs) {
                    LCTX.e("Pool: " + ref);
                }
                sum += ref.size;
            }
        }
        for (final BitmapRef ref : used.values()) {
            if (!ref.isRecycled()) {
                if (showRefs) {
                    LCTX.e("Used: " + ref);
                }
                sum += ref.size;
            }
        }
        if (showRefs) {
            LCTX.e(msg + "Bitmaps&NativeHeap : " + sum + "(" + used.size() + " used / " + pool.size() + " pooled)/"
                    + Debug.getNativeHeapAllocatedSize() + "/" + Debug.getNativeHeapSize());
        } else {
            if (LCTX.isDebugEnabled()) {
                LCTX.d(msg + "Bitmaps&NativeHeap : " + sum + "(" + used.size() + " used / " + pool.size() + " pooled)/"
                        + Debug.getNativeHeapAllocatedSize() + "/" + Debug.getNativeHeapSize());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void release() {
        generation.incrementAndGet();
        removeOldRefs();

        int count = 0;
        final int queueBefore = LCTX.isDebugEnabled() ? releasing.size() : 0;
        while (!releasing.isEmpty()) {
            final Object ref = releasing.poll();
            if (ref instanceof BitmapRef) {
                releaseImpl((BitmapRef) ref);
                count++;
            } else if (ref instanceof List) {
                final List<Bitmaps> list = (List<Bitmaps>) ref;
                for (final Bitmaps bmp : list) {
                    final BitmapRef[] bitmaps = bmp.clear();
                    if (bitmaps != null) {
                        for (final BitmapRef bitmap : bitmaps) {
                            if (bitmap != null) {
                                releaseImpl(bitmap);
                                count++;
                            }
                        }
                    }
                }
            } else {
                LCTX.e("Unknown object in release queue: " + ref);
            }
        }

        shrinkPool(BITMAP_MEMORY_LIMIT);

        if (LCTX.isDebugEnabled()) {
            LCTX.d("Return " + count + " bitmap(s) to pool: " + "memoryUsed=" + used.size() + "/"
                    + (memoryUsed.get() / 1024) + "KB" + ", memoryInPool=" + pool.size() + "/"
                    + (memoryPooled.get() / 1024) + "KB" + ", releasing queue size " + queueBefore + " => 0");
        }
        print("After  release: ", false);
    }

    public static void release(final BitmapRef ref) {
        if (ref != null) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Adding 1 ref to release queue");
            }
            releasing.add(ref);
        }
    }

    public static void release(final List<Bitmaps> bitmapsToRecycle) {
        if (LengthUtils.isNotEmpty(bitmapsToRecycle)) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Adding  list of " + bitmapsToRecycle.size() + " bitmaps to release queue");
            }
            releasing.add(new ArrayList<Bitmaps>(bitmapsToRecycle));
        }
    }

    static void releaseImpl(final BitmapRef ref) {
        assert ref != null;

        if (null != used.remove(ref.id)) {
            memoryUsed.addAndGet(-ref.size);
        } else {
            LCTX.e("The bitmap " + ref + " not found in used ones");
        }

        pool.add(ref);
        memoryPooled.addAndGet(ref.size);
    }

    private static void removeOldRefs() {
        final long gen = generation.get();
        int recycled = 0;
        final Iterator<BitmapRef> it = pool.iterator();
        while (it.hasNext()) {
            final BitmapRef ref = it.next();
            if (gen - ref.gen > GENERATION_THRESHOLD) {
                it.remove();
                ref.recycle();
                recycled++;
                memoryPooled.addAndGet(-ref.size);
            }
        }
        if (recycled > 0) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Recycled " + recycled + " pooled bitmap(s): " + "memoryUsed=" + used.size() + "/"
                        + (memoryUsed.get() / 1024) + "KB" + ", memoryInPool=" + pool.size() + "/"
                        + (memoryPooled.get() / 1024) + "KB");
            }
        }
    }

    private static void shrinkPool(final long limit) {
        int recycled = 0;
        while (memoryPooled.get() + memoryUsed.get() > limit && !pool.isEmpty()) {
            final BitmapRef ref = pool.poll();
            if (ref != null) {
                ref.recycle();
                memoryPooled.addAndGet(-ref.size);
                recycled++;
            }
        }

        if (recycled > 0) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Recycled " + recycled + " pooled bitmap(s): " + "memoryUsed=" + used.size() + "/"
                        + (memoryUsed.get() / 1024) + "KB" + ", memoryInPool=" + pool.size() + "/"
                        + (memoryPooled.get() / 1024) + "KB");
            }
        }
    }

    public static int getBitmapBufferSize(final int width, final int height, final Bitmap.Config config) {
        return getPixelSizeInBytes(config) * width * height;
    }

    public static int getBitmapBufferSize(final Bitmap parentBitmap, final Rect childSize) {
        int bytes = 4;
        if (parentBitmap != null) {
            bytes = BitmapManager.getPixelSizeInBytes(parentBitmap.getConfig());
        }
        return bytes * childSize.width() * childSize.height();
    }

    public static int getPartSize() {
        return partSize;
    }

    public static void setPartSize(final int partSize) {
        BitmapManager.partSize = partSize;
    }

    public static boolean isUseEarlyRecycling() {
        return useEarlyRecycling;
    }

    public static void setUseEarlyRecycling(final boolean useEarlyRecycling) {
        BitmapManager.useEarlyRecycling = useEarlyRecycling;
    }

    public static void setUseBitmapHack(final boolean useBitmapHack) {
        BitmapManager.useBitmapHack = useBitmapHack;
    }

    public static int getPixelSizeInBytes(final Bitmap.Config config) {
        switch (config) {
            case ALPHA_8:
                return 1;
            case ARGB_4444:
            case RGB_565:
                return 2;
            case ARGB_8888:
            default:
                return 4;
        }
    }
}
