package org.ebookdroid.core.bitmaps;

import org.ebookdroid.core.log.LogContext;

import android.graphics.Bitmap;
import android.graphics.Rect;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BitmapManager {

    private static final LogContext LCTX = LogContext.ROOT.lctx("Imaging");

    private static final int BITMAP_MEMORY_LIMIT = 8 * 1024 * 1024;

    private static Map<Integer, BitmapRef> actual = new HashMap<Integer, BitmapRef>();

    private static LinkedList<BitmapRef> bitmaps = new LinkedList<BitmapRef>();

    private static long created;
    private static long reused;

    private static long memoryUsed;
    private static long memoryPooled;

    private static long generation;

    public static synchronized void increateGeneration() {
        generation++;
    }

    public static synchronized BitmapRef getBitmap(final int width, final int height, final Bitmap.Config config) {

        removeBadRefs();

        final Iterator<BitmapRef> it = bitmaps.iterator();
        while (it.hasNext()) {
            final BitmapRef ref = it.next();
            final Bitmap bmp = ref.getBitmap();

            if (bmp == null) {
                it.remove();
                memoryPooled -= ref.size;
                continue;
            }

            if (bmp.getConfig() == config && bmp.getWidth() == width && bmp.getHeight() >= height) {
                it.remove();
                reused++;

                ref.restoreDirectRef(bmp, generation);

                memoryPooled -= ref.size;
                memoryUsed += ref.size;

                if (LCTX.isDebugEnabled()) {
                    LCTX.d("Reuse bitmap from pool: [" + Integer.toHexString(System.identityHashCode(bmp)) + ", "
                            + width + ", " + height + "], created=" + created + ", reused=" + reused + ", memoryUsed="
                            + (memoryUsed / 1024) + "KB" + ", memoryInPool=" + (memoryPooled / 1024) + "KB");
                }
                return ref;
            }
        }

        final BitmapRef ref = new BitmapRef(Bitmap.createBitmap(width, height, config), generation);
        actual.put(ref.id, ref);

        created++;
        memoryUsed += ref.size;

        if (LCTX.isDebugEnabled()) {
            LCTX.d("Create new bitmap: [" + Integer.toHexString(System.identityHashCode(ref.bitmap)) + ", " + width
                    + ", " + height + "], created=" + created + ", reused=" + reused + ", memoryUsed="
                    + (memoryUsed / 1024) + "KB" + ", memoryInPool=" + (memoryPooled / 1024) + "KB");
        }

        return ref;
    }

    public static synchronized void release(final List<BitmapRef> bitmapsToRecycle) {
        if (bitmapsToRecycle.size() > 0) {
            removeOldRefs();
            removeBadRefs();

            for (final BitmapRef bitmap : bitmapsToRecycle) {
                releaseImpl(bitmap);
            }

            if (LCTX.isDebugEnabled()) {
                LCTX.d("Return " + bitmapsToRecycle.size() + " bitmap(s) to pool: " + "memoryUsed="
                        + (memoryUsed / 1024) + "KB" + ", memoryInPool=" + (memoryPooled / 1024) + "KB");
            }

            bitmapsToRecycle.clear();
            shrinkPool();
        }
    }

    public static synchronized void release(final BitmapRef ref) {

        removeOldRefs();
        removeBadRefs();

        releaseImpl(ref);

        if (LCTX.isDebugEnabled()) {
            LCTX.d("Return 1 bitmap(s) to pool: " + "memoryUsed=" + (memoryUsed / 1024) + "KB" + ", memoryInPool="
                    + (memoryPooled / 1024) + "KB");
        }

        shrinkPool();
    }

    private static void releaseImpl(final BitmapRef ref) {
        if (ref != null) {
            ref.clearDirectRef();
            actual.remove(ref.id);
            bitmaps.add(ref);
            memoryPooled += ref.size;
            memoryUsed -= ref.size;
        }
    }

    private static void removeOldRefs() {
        int recycled = 0;
        final Iterator<BitmapRef> it = bitmaps.iterator();
        while (it.hasNext()) {
            final BitmapRef ref = it.next();
            final Bitmap bmp = ref.ref.get();
            if (bmp == null) {
                it.remove();
                recycled++;
                memoryPooled -= ref.size;
            } else if (generation - ref.gen > 5) {
                it.remove();
                recycled++;
                memoryPooled -= ref.size;
                ref.recycle();
            }
        }
        if (recycled > 0) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Recycled " + recycled + " bitmap(s):" + " memoryUsed=" + (memoryUsed / 1024) + "KB"
                        + ", memoryInPool=" + (memoryPooled / 1024) + "KB");
            }
        }
    }

    private static void removeBadRefs() {
        int recycled = 0;
        final Iterator<BitmapRef> it = actual.values().iterator();
        while (it.hasNext()) {
            final BitmapRef ref = it.next();
            final Bitmap bmp = ref.getBitmap();
            if (bmp == null) {
                it.remove();
                recycled++;
                memoryUsed -= ref.size;
            }
        }
        if (recycled > 0) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Recycled " + recycled + " bitmap(s):" + " memoryUsed=" + (memoryUsed / 1024) + "KB"
                        + ", memoryInPool=" + (memoryPooled / 1024) + "KB");
            }
        }
    }

    private static void shrinkPool() {
        int recycled = 0;
        while (memoryPooled > BITMAP_MEMORY_LIMIT && !bitmaps.isEmpty()) {
            final BitmapRef ref = bitmaps.removeFirst();
            ref.recycle();
            memoryPooled -= ref.size;
            recycled++;
        }

        if (recycled > 0) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Recycled " + recycled + " bitmap(s):" + " memoryUsed=" + (memoryUsed / 1024) + "KB"
                        + ", memoryInPool=" + (memoryPooled / 1024) + "KB");
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
