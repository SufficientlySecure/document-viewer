package org.ebookdroid.utils;

import android.graphics.Bitmap;

import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.LinkedList;


public class BitmapManager {
    private static final int BITMAP_POOL_SIZE = 10;

    private static LinkedList<SoftReference<Bitmap>> bitmaps = new LinkedList<SoftReference<Bitmap>>();
    
    public static synchronized Bitmap getBitmap(final int width, final int height, final Bitmap.Config config) {
        Iterator<SoftReference<Bitmap>> it = bitmaps.iterator();
        while(it.hasNext()) {
            SoftReference<Bitmap> ref = it.next();
            if (ref == null) {
                it.remove();
                continue;
            }
            Bitmap bmp = ref.get();
            if (bmp == null) {
                it.remove();
                continue;
            }
            if (bmp.getConfig() == config && bmp.getWidth() == width && bmp.getHeight() >= height) {
                it.remove();
                return bmp;
            }
        }
        
        return Bitmap.createBitmap(width, height, config);
    }
    
    public static synchronized void recycle(Bitmap bitmap) {
        bitmaps.addFirst(new SoftReference<Bitmap>(bitmap));
        if (bitmaps.size() > BITMAP_POOL_SIZE) {
            SoftReference<Bitmap> ref = bitmaps.removeLast();
            if (ref != null) {
                Bitmap bmp = ref.get();
                if (bmp != null) {
                    bmp.recycle();
                    bmp = null;
                }
            }
        }
    }
}
