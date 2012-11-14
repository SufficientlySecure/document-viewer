package org.ebookdroid.common.bitmaps;

import org.ebookdroid.core.PagePaint;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

import org.emdev.ui.gl.BitmapTexture;
import org.emdev.ui.gl.GLCanvas;
import org.emdev.utils.MathUtils;

public class GLBitmaps extends Bitmaps {

    protected BitmapTexture[] textures;

    public GLBitmaps(final String nodeId, final IBitmapRef orig, final Rect bitmapBounds, final boolean invert) {
        super(nodeId, orig, bitmapBounds, invert);
    }

    @Override
    public boolean reuse(final String nodeId, final IBitmapRef orig, final Rect bitmapBounds, final boolean invert) {
        return false;
    }

    @Override
    public boolean hasBitmaps() {
        lock.readLock().lock();
        try {
            if (textures != null) {
                return true;
            }
            if (bitmaps == null) {
                return false;
            }
            for (int i = 0; i < bitmaps.length; i++) {
                if (bitmaps[i] == null) {
                    return false;
                }
                if (bitmaps[i].isRecycled()) {
                    return false;
                }
            }
            return true;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    AbstractBitmapRef[] clear() {
        lock.writeLock().lock();
        try {
            if (textures != null) {
                for (int i = 0; i < textures.length; i++) {
                    textures[i].recycle();
                }
                textures = null;
            }

            final AbstractBitmapRef[] refs = this.bitmaps;
            this.bitmaps = null;
            return refs;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        lock.writeLock().lock();
        try {
            if (textures != null) {
                for (int i = 0; i < textures.length; i++) {
                    textures[i].recycle();
                }
                textures = null;
            }
            if (bitmaps != null) {
                for (final AbstractBitmapRef ref : bitmaps) {
                    BitmapManager.release(ref);
                }
                bitmaps = null;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean drawGL(final GLCanvas canvas, final PagePaint paint, final PointF vb, final RectF tr, final RectF cr) {
        lock.writeLock().lock();
        try {
            if (textures == null) {
                if (this.bitmaps == null) {
                    return false;
                }
                textures = new BitmapTexture[bitmaps.length];
                for (int i = 0; i < textures.length; i++) {
                    textures[i] = new BitmapTexture(bitmaps[i].getBitmap());
                }
            }

            if (LCTX.isDebugEnabled()) {
                LCTX.d(nodeId + ".drawGL(): >>>>");
            }

            final RectF actual = new RectF(cr.left - vb.x, cr.top - vb.y, cr.right - vb.x, cr.bottom - vb.y);
            MathUtils.round(actual);
            canvas.setClipRect(actual);

            final float offsetX = tr.left - vb.x;
            final float offsetY = tr.top - vb.y;

            final float scaleX = tr.width() / bounds.width();
            final float scaleY = tr.height() / bounds.height();

            final float sizeX = partSize * scaleX;
            final float sizeY = partSize * scaleY;

            final RectF src = new RectF();
            final RectF rect = new RectF(offsetX, offsetY, offsetX + sizeX, offsetY + sizeY);
            final RectF r = new RectF();

            boolean res = true;
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < columns; col++) {
                    final int index = row * columns + col;
                    final BitmapTexture t = this.textures[index];
                    if (t != null) {
                        r.set(rect);
                        src.set(0, 0, t.getWidth(), t.getHeight());
                        if (r.right < actual.left || r.left > actual.right || r.bottom < actual.top
                                || r.top > actual.bottom) {

                        } else {
                            final boolean tres = canvas.drawTexture(t, src, r);
                            if (LCTX.isDebugEnabled()) {
                                LCTX.d(nodeId + ": " + row + "." + col + " : " + t.getId() + " = " + tres);
                            }
                            res &= tres;
                        }
                    } else {
                        res = false;
                    }
                    rect.left += sizeX;
                    rect.right += sizeX;
                }
                rect.left = offsetX;
                rect.right = offsetX + sizeX;

                rect.top += sizeY;
                rect.bottom += sizeY;
            }

            if (res && bitmaps != null) {
                BitmapManager.release(bitmaps);
                bitmaps = null;
            }

            if (LCTX.isDebugEnabled()) {
                LCTX.d(nodeId + ".drawGL(): <<<<<");
            }

            canvas.clearClipRect();

            return res;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
