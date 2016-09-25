package org.ebookdroid.core;

import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.ByteBufferBitmap;
import org.ebookdroid.common.bitmaps.ByteBufferManager;
import org.ebookdroid.common.bitmaps.GLBitmaps;
import org.ebookdroid.common.cache.DocumentCacheFile.PageInfo;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.models.DecodingProgressModel;
import org.ebookdroid.ui.viewer.IViewController;

import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.emdev.common.log.LogContext;
import org.emdev.ui.gl.GLCanvas;
import org.emdev.utils.MatrixUtils;

public class PageTreeNode implements DecodeService.DecodeCallback {

    private static final LogContext LCTX = Page.LCTX;

    final Page page;
    final PageTreeNode parent;
    final int id;
    final PageTreeLevel level;
    final String shortId;
    final String fullId;

    final AtomicBoolean decodingNow = new AtomicBoolean();
    final BitmapHolder holder = new BitmapHolder();

    final RectF localPageSliceBounds;
    final RectF pageSliceBounds;

    float bitmapZoom = 1;
    private RectF autoCropping = null;
    private RectF manualCropping = null;

    public RectF getCropping() {
        return manualCropping != null ? manualCropping : autoCropping;
    }

    public boolean hasManualCropping() {
        return manualCropping != null;
    }

    public void setInitialCropping(final PageInfo pi) {
        if (id != 0) {
            return;
        }

        if (pi != null) {
            autoCropping = pi.autoCropping != null ? new RectF(pi.autoCropping) : null;
            manualCropping = pi.manualCropping != null ? new RectF(pi.manualCropping) : null;
        } else {
            autoCropping = null;
            manualCropping = null;
        }

        page.updateAspectRatio();
    }

    public void setAutoCropping(final RectF r, final boolean commit) {
        autoCropping = r;
        if (id == 0) {
            if (commit) {
                page.base.getDocumentModel().updateAutoCropping(page, r);
            }
            page.updateAspectRatio();
        }
    }

    public void setManualCropping(final RectF r, final boolean commit) {
        manualCropping = r;
        if (id == 0) {
            if (commit) {
                page.base.getDocumentModel().updateManualCropping(page, r);
            }
            page.updateAspectRatio();
        }
    }

    PageTreeNode(final Page page) {
        assert page != null;

        this.page = page;
        this.parent = null;
        this.id = 0;
        this.level = PageTreeLevel.ROOT;
        this.shortId = page.index.viewIndex + ":0";
        this.fullId = page.index + ":0";
        this.localPageSliceBounds = page.type.getInitialRect();
        this.pageSliceBounds = localPageSliceBounds;
        this.autoCropping = null;
        this.manualCropping = null;
    }

    PageTreeNode(final Page page, final PageTreeNode parent, final int id, final RectF localPageSliceBounds) {
        assert id != 0;
        assert page != null;
        assert parent != null;

        this.page = page;
        this.parent = parent;
        this.id = id;
        this.level = parent.level.next;
        this.shortId = page.index.viewIndex + ":" + id;
        this.fullId = page.index + ":" + id;
        this.localPageSliceBounds = localPageSliceBounds;
        this.pageSliceBounds = evaluatePageSliceBounds(localPageSliceBounds, parent);

        evaluateCroppedPageSliceBounds();
    }

    @Override
    protected void finalize() throws Throwable {
        holder.recycle(null);
    }

    public boolean recycle(final List<GLBitmaps> bitmapsToRecycle) {
        stopDecodingThisNode("node recycling");
        return holder.recycle(bitmapsToRecycle);
    }

    protected void decodePageTreeNode(final List<PageTreeNode> nodesToDecode, final ViewState viewState) {
        if (this.decodingNow.compareAndSet(false, true)) {
            bitmapZoom = viewState.zoom;
            nodesToDecode.add(this);
        }
    }

    void stopDecodingThisNode(final String reason) {
        if (this.decodingNow.compareAndSet(true, false)) {
            final DecodingProgressModel dpm = page.base.getDecodingProgressModel();
            if (dpm != null) {
                dpm.decrease();
            }
            if (reason != null) {
                final DecodeService ds = page.base.getDecodeService();
                if (ds != null) {
                    ds.stopDecoding(this, reason);
                }
            }
        }
    }

    @Override
    public void decodeComplete(final CodecPage codecPage, final ByteBufferBitmap bitmap, final RectF croppedPageBounds) {

        try {
            if (bitmap == null) {
                stopDecodingThisNode(null);
                return;
            }

            final AppSettings app = AppSettings.current();
            final BookSettings bs = page.base.getBookSettings();
            final boolean invert = bs != null ? bs.nightMode : app.nightMode;
            final boolean tint = bs != null ? bs.tint : app.tint;
            final int tintColor = bs != null ? bs.tintColor : app.tintColor;

            if (bs != null) {
                bitmap.applyEffects(bs);
            }
            if (invert) {
                bitmap.invert();
            }

            final PagePaint paint = tint ? PagePaint.TintedDay(tintColor)
                    : (invert ? PagePaint.Night()
                        : PagePaint.Day());

            final GLBitmaps bitmaps = new GLBitmaps(fullId, bitmap, paint);

            holder.setBitmap(bitmaps);
            stopDecodingThisNode(null);

            final IViewController dc = page.base.getDocumentController();
            if (dc instanceof AbstractViewController) {
                EventPool.newEventChildLoaded((AbstractViewController) dc, PageTreeNode.this).process()
                        .release();
            }

        } catch (final OutOfMemoryError ex) {
            LCTX.e("No memory: ", ex);
            BitmapManager.clear("PageTreeNode OutOfMemoryError: ");
            ByteBufferManager.clear("PageTreeNode OutOfMemoryError: ");
            stopDecodingThisNode(null);
        } finally {
            ByteBufferManager.release(bitmap);
        }

    }

    public RectF getTargetRect(final RectF pageBounds) {
        return Page.getTargetRect(page.type, pageBounds, pageSliceBounds);
    }

    public static RectF evaluatePageSliceBounds(final RectF localPageSliceBounds, final PageTreeNode parent) {
        final Matrix tmpMatrix = MatrixUtils.get();

        tmpMatrix.postScale(parent.pageSliceBounds.width(), parent.pageSliceBounds.height());
        tmpMatrix.postTranslate(parent.pageSliceBounds.left, parent.pageSliceBounds.top);
        final RectF sliceBounds = new RectF();
        tmpMatrix.mapRect(sliceBounds, localPageSliceBounds);
        return sliceBounds;
    }

    public void evaluateCroppedPageSliceBounds() {
        if (parent == null) {
            return;
        }

        if (parent.getCropping() == null) {
            parent.evaluateCroppedPageSliceBounds();
        }

        autoCropping = evaluateCroppedPageSliceBounds(parent.autoCropping, this.localPageSliceBounds);
        manualCropping = evaluateCroppedPageSliceBounds(parent.manualCropping, this.localPageSliceBounds);
    }

    public static RectF evaluateCroppedPageSliceBounds(final RectF crop, final RectF slice) {
        if (crop == null) {
            return null;
        }

        final RectF sliceBounds = new RectF();
        final Matrix tmpMatrix = MatrixUtils.get();

        tmpMatrix.postScale(crop.width(), crop.height());
        tmpMatrix.postTranslate(crop.left, crop.top);
        tmpMatrix.mapRect(sliceBounds, slice);
        return sliceBounds;
    }

    @Override
    public int hashCode() {
        return (page == null) ? 0 : page.index.viewIndex;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof PageTreeNode) {
            final PageTreeNode that = (PageTreeNode) obj;
            if (this.page == null) {
                return that.page == null;
            }
            return this.page.index.viewIndex == that.page.index.viewIndex
                    && this.pageSliceBounds.equals(that.pageSliceBounds);
        }

        return false;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder("PageTreeNode");
        buf.append("[");

        buf.append("id").append("=").append(page.index.viewIndex).append(":").append(id);
        buf.append(", ");
        buf.append("rect").append("=").append(this.pageSliceBounds);
        buf.append(", ");
        buf.append("hasBitmap").append("=").append(holder.hasBitmaps());

        buf.append("]");
        return buf.toString();
    }

    class BitmapHolder {

        final AtomicReference<GLBitmaps> ref = new AtomicReference<GLBitmaps>();

        public boolean drawBitmap(final GLCanvas canvas, final PagePaint paint, final PointF viewBase,
                final RectF targetRect, final RectF clipRect) {
            final GLBitmaps bitmaps = ref.get();
            return bitmaps != null ? bitmaps.drawGL(canvas, paint, viewBase, targetRect, clipRect) : false;
        }

        public boolean hasBitmaps() {
            final GLBitmaps bitmaps = ref.get();
            return bitmaps != null ? bitmaps.hasBitmaps() : false;
        }

        public boolean recycle(final List<GLBitmaps> bitmapsToRecycle) {
            final GLBitmaps bitmaps = ref.getAndSet(null);
            if (bitmaps != null) {
                if (bitmapsToRecycle != null) {
                    bitmapsToRecycle.add(bitmaps);
                } else {
                    ByteBufferManager.release(bitmaps);
                }
                return true;
            }
            return false;
        }

        public void setBitmap(final GLBitmaps bitmaps) {
            if (bitmaps == null) {
                return;
            }
            final GLBitmaps oldBitmaps = ref.getAndSet(bitmaps);
            if (oldBitmaps != null && oldBitmaps != bitmaps) {
                ByteBufferManager.release(oldBitmaps);
            }
        }
    }
}
