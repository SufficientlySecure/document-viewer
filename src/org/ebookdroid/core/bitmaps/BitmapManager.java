package org.ebookdroid.core.bitmaps;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.core.log.LogContext;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BitmapManager {

    private static final LogContext LCTX = LogContext.ROOT.lctx("Imaging", false);

    private final static long BITMAP_MEMORY_LIMIT = Runtime.getRuntime().maxMemory() / 2;

    private static Map<Integer, BitmapRef> used = new HashMap<Integer, BitmapRef>();

    private static LinkedList<BitmapRef> pool = new LinkedList<BitmapRef>();

    private static SparseArray<Bitmap> resources = new SparseArray<Bitmap>();

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
        if (bitmap == null) {
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
                    LCTX.d("Reuse bitmap: [" + name + ", " + width + ", " + height + "], created=" + created
                            + ", reused=" + reused + ", memoryUsed=" + used.size() + "/" + (memoryUsed / 1024) + "KB"
                            + ", memoryInPool=" + pool.size() + "/" + (memoryPooled / 1024) + "KB");
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
            LCTX.d("Create bitmap: [" + name + ", " + width + ", " + height + "], created=" + created + ", reused="
                    + reused + ", memoryUsed=" + used.size() + "/" + (memoryUsed / 1024) + "KB" + ", memoryInPool="
                    + pool.size() + "/" + (memoryPooled / 1024) + "KB");
        }

        shrinkPool(BITMAP_MEMORY_LIMIT);

        ref.name = name;
        return ref;
    }

    public static synchronized void clear() {
        generation += 10;
        removeOldRefs();
        removeEmptyRefs();
        shrinkPool(0);
        for (final BitmapRef ref : used.values()) {
            LCTX.e("Used: " + ref);
        }
    }

    public static synchronized void release(final List<BitmapRef> bitmapsToRecycle) {
        if (bitmapsToRecycle.size() > 0) {
            removeOldRefs();
            removeEmptyRefs();

            for (final BitmapRef bitmap : bitmapsToRecycle) {
                releaseImpl(bitmap);
            }

            if (LCTX.isDebugEnabled()) {
                LCTX.d("Return " + bitmapsToRecycle.size() + " bitmap(s) to pool: " + "memoryUsed=" + used.size() + "/"
                        + (memoryUsed / 1024) + "KB" + ", memoryInPool=" + pool.size() + "/" + (memoryPooled / 1024)
                        + "KB");
            }

            bitmapsToRecycle.clear();

            shrinkPool(BITMAP_MEMORY_LIMIT);
        }
    }

    public static synchronized void release(final BitmapRef ref) {
        removeOldRefs();
        removeEmptyRefs();

        releaseImpl(ref);

        if (LCTX.isDebugEnabled()) {
            LCTX.d("Return 1 bitmap(s) to pool: " + "memoryUsed=" + used.size() + "/" + (memoryUsed / 1024) + "KB"
                    + ", memoryInPool=" + pool.size() + "/" + (memoryPooled / 1024) + "KB");
        }

        shrinkPool(BITMAP_MEMORY_LIMIT);
    }

    private static void releaseImpl(final BitmapRef ref) {
        if (ref != null) {
            if (null != used.remove(ref.id)) {
                memoryUsed -= ref.size;
            }
            ref.clearDirectRef();
            if (!ref.clearEmptyRef()) {
                pool.add(ref);
                memoryPooled += ref.size;
            }
        }
    }

    private static void removeOldRefs() {
        int recycled = 0;
        final Iterator<BitmapRef> it = pool.iterator();
        while (it.hasNext()) {
            final BitmapRef ref = it.next();
            if (ref.clearEmptyRef()) {
                it.remove();
                recycled++;
                memoryPooled -= ref.size;
            } else if (generation - ref.gen > 5) {
                ref.recycle();
                it.remove();
                recycled++;
                memoryPooled -= ref.size;
            }
        }
        if (recycled > 0) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Recycled " + recycled + " pooled bitmap(s): " + "memoryUsed=" + used.size() + "/"
                        + (memoryUsed / 1024) + "KB" + ", memoryInPool=" + pool.size() + "/" + (memoryPooled / 1024)
                        + "KB");
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
