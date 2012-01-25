package org.ebookdroid.core.bitmaps;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.utils.LengthUtils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Debug;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BitmapManager {

    static final LogContext LCTX = LogContext.ROOT.lctx("BitmapManager", false);

    private final static long BITMAP_MEMORY_LIMIT = Runtime.getRuntime().maxMemory() / 2;

    private static Map<Integer, BitmapRef> used = new HashMap<Integer, BitmapRef>();

    private static LinkedList<BitmapRef> pool = new LinkedList<BitmapRef>();

    private static SparseArray<Bitmap> resources = new SparseArray<Bitmap>();

    private static ConcurrentLinkedQueue<Object> releasing = new ConcurrentLinkedQueue<Object>();

    private static long created;
    private static long reused;

    private static long memoryUsed;
    private static long memoryPooled;

    private static long generation;

    public static synchronized void increateGeneration() {
        generation++;
    }

    public static synchronized Bitmap getResource(final int resourceId) {
        Bitmap bitmap = resources.get(resourceId);
        if (bitmap == null || bitmap.isRecycled()) {
            final Resources resources = EBookDroidApp.getAppContext().getResources();
            bitmap = BitmapFactory.decodeResource(resources, resourceId);
        }
        return bitmap;
    }

    public static synchronized BitmapRef getBitmap(final String name, final int width, final int height,
            final Bitmap.Config config) {
        if (used.size() == 0 && pool.size() == 0) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("!!! Bitmap pool size: " + (BITMAP_MEMORY_LIMIT / 1024) + "KB");
            }
        } else {
            removeOldRefs();
            removeEmptyRefs();
        }

        final Iterator<BitmapRef> it = pool.iterator();
        while (it.hasNext()) {
            final BitmapRef ref = it.next();
            final Bitmap bmp = ref.getBitmap();

            if (bmp != null && bmp.getConfig() == config && bmp.getWidth() == width && bmp.getHeight() >= height) {
                it.remove();

                ref.restoreDirectRef(bmp, generation);
                used.put(ref.id, ref);

                reused++;
                memoryPooled -= ref.size;
                memoryUsed += ref.size;

                if (LCTX.isDebugEnabled()) {
                    LCTX.d("Reuse bitmap: [" + ref.id + ", " + ref.name + " => " + name + ", " + width + ", " + height
                            + "], created=" + created + ", reused=" + reused + ", memoryUsed=" + used.size() + "/"
                            + (memoryUsed / 1024) + "KB" + ", memoryInPool=" + pool.size() + "/"
                            + (memoryPooled / 1024) + "KB");
                }
                bmp.eraseColor(Color.CYAN);
                ref.name = name;
                return ref;
            }
        }

        final BitmapRef ref = new BitmapRef(Bitmap.createBitmap(width, height, config), generation);
        used.put(ref.id, ref);

        created++;
        memoryUsed += ref.size;

        if (LCTX.isDebugEnabled()) {
            LCTX.d("Create bitmap: [" + ref.id + ", " + name + ", " + width + ", " + height + "], created=" + created
                    + ", reused=" + reused + ", memoryUsed=" + used.size() + "/" + (memoryUsed / 1024) + "KB"
                    + ", memoryInPool=" + pool.size() + "/" + (memoryPooled / 1024) + "KB");
        }

        shrinkPool(BITMAP_MEMORY_LIMIT);

        ref.name = name;
        return ref;
    }

    public static synchronized void clear(final String msg) {
        generation += 10;
        removeOldRefs();
        removeEmptyRefs();
        release();
        shrinkPool(0);
        print(msg, true);
    }

    private static void print(final String msg, final boolean showRefs) {
        long sum = 0;
        for (final BitmapRef ref : pool) {
            if (!ref.clearEmptyRef()) {
                if (showRefs) {
                    LCTX.e("Pool: " + ref);
                }
                sum += ref.size;
            }
        }
        for (final BitmapRef ref : used.values()) {
            if (!ref.clearEmptyRef()) {
                if (showRefs) {
                    LCTX.e("Used: " + ref);
                }
                sum += ref.size;
            }
        }
        LCTX.e(msg + "Bitmaps&NativeHeap : " + sum + "(" + (pool.size() + used.size()) + " instances)/"
                + Debug.getNativeHeapAllocatedSize() + "/" + Debug.getNativeHeapSize());
    }

    @SuppressWarnings("unchecked")
    public static synchronized void release() {
        increateGeneration();
        removeOldRefs();
        removeEmptyRefs();
        int count = 0;
        final int queueBefore = releasing.size();
        while (!releasing.isEmpty()) {
            final Object ref = releasing.poll();
            if (ref instanceof BitmapRef) {
                releaseImpl((BitmapRef) ref);
                count++;
            } else if (ref instanceof List) {
                final List<BitmapRef> list = (List<BitmapRef>) ref;
                for (final BitmapRef bitmap : list) {
                    releaseImpl(bitmap);
                    count++;
                }
            } else {
                LCTX.e("Unknown object in release queue: " + ref);
            }
        }
        shrinkPool(BITMAP_MEMORY_LIMIT);
        if (LCTX.isDebugEnabled()) {
            LCTX.d("Return " + count + " bitmap(s) to pool: " + "memoryUsed=" + used.size() + "/" + (memoryUsed / 1024)
                    + "KB" + ", memoryInPool=" + pool.size() + "/" + (memoryPooled / 1024) + "KB"
                    + ", releasing queue size " + queueBefore + " => 0");
        }
        print("After  release: ", false);
    }

    public static void release(final List<BitmapRef> bitmapsToRecycle) {
        if (LengthUtils.isNotEmpty(bitmapsToRecycle)) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Adding  list of " + bitmapsToRecycle.size() + " refs to release queue");
            }
            releasing.add(new ArrayList<BitmapRef>(bitmapsToRecycle));
        }
    }

    public static void release(final BitmapRef[] bitmapsToRecycle) {
        if (LengthUtils.isNotEmpty(bitmapsToRecycle)) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Adding array of " + bitmapsToRecycle.length + " refs to release queue");
            }
            releasing.add(Arrays.asList(bitmapsToRecycle));
        }
    }

    public static void release(final BitmapRef ref) {
        if (ref != null) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Adding 1 ref to release queue");
            }
            releasing.add(ref);
        }
    }

    private static void releaseImpl(final BitmapRef ref) {
        if (ref != null) {
            if (null != used.remove(ref.id)) {
                memoryUsed -= ref.size;
            } else {
                LCTX.e("The bitmap " + ref + " not found in used ones");
            }
            if (generation - ref.gen > 5) {
                ref.clearDirectRef();
            }
            if (!ref.clearEmptyRef()) {
                pool.add(ref);
                memoryPooled += ref.size;
            }
        }
    }

    private static void removeOldRefs() {
        int recycled = 0;
        int invalid = 0;
        final Iterator<BitmapRef> it = pool.iterator();
        while (it.hasNext()) {
            final BitmapRef ref = it.next();
            if (ref.clearEmptyRef()) {
                it.remove();
                invalid++;
                memoryPooled -= ref.size;
            } else if (generation - ref.gen > 5) {
                ref.clearDirectRef();
                if (ref.clearEmptyRef()) {
                    it.remove();
                    recycled++;
                    memoryPooled -= ref.size;
                }
            }
        }
        if (recycled + invalid > 0) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Recycled " + invalid + "/" + recycled + " pooled bitmap(s): " + "memoryUsed=" + used.size()
                        + "/" + (memoryUsed / 1024) + "KB" + ", memoryInPool=" + pool.size() + "/"
                        + (memoryPooled / 1024) + "KB");
            }
        }
    }

    private static void removeEmptyRefs() {
        int recycled = 0;
        final Iterator<BitmapRef> it = used.values().iterator();
        while (it.hasNext()) {
            final BitmapRef ref = it.next();
            if (ref.clearEmptyRef()) {
                it.remove();
                recycled++;
                memoryUsed -= ref.size;
            }
        }
        if (recycled > 0) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Removed " + recycled + " autorecycled bitmap(s): " + "memoryUsed=" + used.size() + "/"
                        + (memoryUsed / 1024) + "KB" + ", memoryInPool=" + pool.size() + "/" + (memoryPooled / 1024)
                        + "KB");
            }
        }
    }

    private static void shrinkPool(final long limit) {
        int recycled = 0;
        while (memoryPooled + memoryUsed > limit && !pool.isEmpty()) {
            final BitmapRef ref = pool.removeFirst();
            ref.recycle();
            memoryPooled -= ref.size;
            recycled++;
        }

        if (recycled > 0) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Recycled " + recycled + " pooled bitmap(s): " + "memoryUsed=" + used.size() + "/"
                        + (memoryUsed / 1024) + "KB" + ", memoryInPool=" + pool.size() + "/" + (memoryPooled / 1024)
                        + "KB");
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
