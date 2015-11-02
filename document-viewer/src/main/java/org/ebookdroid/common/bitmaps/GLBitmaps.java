package org.ebookdroid.common.bitmaps;

import org.ebookdroid.core.PagePaint;

import android.graphics.PointF;
import android.graphics.RectF;
import android.util.FloatMath;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.ui.gl.GLCanvas;
import org.emdev.utils.MathUtils;

public class GLBitmaps {

    protected static final LogContext LCTX = LogManager.root().lctx("Bitmaps", false);

    protected static final ThreadLocal<ByteBufferBitmap> threadSlices = new ThreadLocal<ByteBufferBitmap>();

    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public final int width;
    public final int height;
    public final String nodeId;
    public final int partSize;
    public final int columns;
    public final int rows;

    private ByteBufferBitmap[] bitmaps;

    private ByteBufferTexture[] textures;

    public GLBitmaps(final String nodeId, final ByteBufferBitmap orig, final PagePaint paint) {
        this.nodeId = nodeId;
        this.width = orig.width;
        this.height = orig.height;
        this.partSize = ByteBufferManager.partSize;
        this.columns = getPartCount(width, partSize);
        this.rows = getPartCount(height, partSize);
        this.bitmaps = ByteBufferManager.getParts(partSize, rows, columns);

        final int color = paint.fillPaint.getColor();

        int top = 0;
        for (int row = 0; row < rows; row++, top += partSize) {
            int left = 0;
            for (int col = 0; col < columns; col++, left += partSize) {
                final int index = row * columns + col;
                try {
                    if (row == rows - 1 || col == columns - 1) {
                        final int right = Math.min(left + partSize, orig.width);
                        final int bottom = Math.min(top + partSize, orig.height);
                        bitmaps[index].eraseColor(color);
                        bitmaps[index].copyPixelsFrom(orig, left, top, right - left, bottom - top);
                    } else {
                        bitmaps[index].copyPixelsFrom(orig, left, top, partSize, partSize);
                    }
                } catch (final IllegalArgumentException ex) {
                    LCTX.e("Cannot create part: " + row + "/" + rows + ", " + col + "/" + columns + ": "
                            + ex.getMessage());
                }
            }
        }
    }

    private static int getPartCount(final int size, final int partSize) {
        if (size % partSize == 0) {
            return size / partSize;
        }
        return (int) Math.ceil(size / (float) partSize);
    }

    public boolean drawGL(final GLCanvas canvas, final PagePaint paint, final PointF vb, final RectF tr, final RectF cr) {
        lock.writeLock().lock();
        try {
            if (textures == null) {
                if (this.bitmaps == null) {
                    return false;
                }
                textures = new ByteBufferTexture[columns * rows];
                for (int i = 0; i < textures.length; i++) {
                    textures[i] = new ByteBufferTexture(bitmaps[i]);
                }
            }

            if (LCTX.isDebugEnabled()) {
                LCTX.d(nodeId + ".drawGL(): >>>>");
            }

            final RectF actual = new RectF(cr.left - vb.x, cr.top - vb.y, cr.right - vb.x, cr.bottom - vb.y);
            MathUtils.round(actual);
            canvas.setClipRect(actual);

            final boolean res = draw(canvas, vb, tr, actual);

            if (res && bitmaps != null) {
                ByteBufferManager.release(bitmaps);
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

    protected boolean draw(final GLCanvas canvas, final PointF vb, final RectF tr, final RectF actual) {
        final float offsetX = tr.left - vb.x;
        final float offsetY = tr.top - vb.y;

        final float scaleX = tr.width() / width;
        final float scaleY = tr.height() / height;

        final float sizeX = partSize * scaleX;
        final float sizeY = partSize * scaleY;

        final RectF src = new RectF();
        final RectF rect = new RectF(offsetX, offsetY, offsetX + sizeX, offsetY + sizeY);
        final RectF r = new RectF();

        boolean res = true;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                final int index = row * columns + col;
                res &= draw(canvas, row, col, this.textures[index], actual, src, rect, r);
                rect.left += sizeX;
                rect.right += sizeX;
            }
            rect.left = offsetX;
            rect.right = offsetX + sizeX;

            rect.top += sizeY;
            rect.bottom += sizeY;
        }
        return res;
    }

    protected boolean draw(final GLCanvas canvas, final int row, final int col, final ByteBufferTexture t,
            final RectF actual, final RectF src, final RectF rect, final RectF r) {
        boolean tres = false;
        if (t != null) {
            r.set(rect);
            src.set(0, 0, t.getWidth(), t.getHeight());
            if (!(r.right < actual.left || r.left > actual.right || r.bottom < actual.top || r.top > actual.bottom)) {
                tres = canvas.drawTexture(t, src, r);
                if (LCTX.isDebugEnabled()) {
                    LCTX.d(nodeId + ": " + row + "." + col + " : " + t.getId() + " = " + tres);
                }
            }
        }
        return tres;
    }

    ByteBufferBitmap[] clear() {
        lock.writeLock().lock();
        try {
            if (textures != null) {
                for (int i = 0; i < textures.length; i++) {
                    textures[i].recycle();
                }
                textures = null;
            }

            final ByteBufferBitmap[] ref = this.bitmaps;
            this.bitmaps = null;
            return ref;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean hasBitmaps() {
        lock.readLock().lock();
        try {
            return textures != null || bitmaps != null;
        } finally {
            lock.readLock().unlock();
        }
    }

}
