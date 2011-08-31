package org.ebookdroid.core;

import org.ebookdroid.core.IDocumentViewController.InvalidateSizeReason;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.models.DecodingProgressModel;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.utils.LengthUtils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import java.lang.ref.SoftReference;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PageTreeNode implements DecodeService.DecodeCallback {

    private static final LogContext LCTX = LogContext.ROOT.lctx("Imaging");

    final Page page;
    final PageTreeNode parent;
    final long id;
    final AtomicBoolean decodingNow = new AtomicBoolean();
    final BitmapHolder holder = new BitmapHolder();

    final float childrenZoomThreshold;
    final RectF pageSliceBounds;
    final Matrix matrix = new Matrix();

    boolean hasChildren = false;

    PageTreeNode(final Page page, final PageTreeNode parent, final long id, final RectF localPageSliceBounds,
            final float childrenZoomThreshold) {
        this.id = id;
        this.parent = parent;
        this.pageSliceBounds = evaluatePageSliceBounds(localPageSliceBounds, parent);
        this.page = page;
        this.childrenZoomThreshold = childrenZoomThreshold;
    }

    public void recycle() {
        stopDecodingThisNode("node recycling");
        holder.recycle();
        hasChildren = page.nodes.recycleChildren(this);
    }

    public boolean onZoomChanged(final float oldZoom, final float newZoom, final RectF viewRect,
            final RectF pageBounds, final List<PageTreeNode> nodesToDecode) {
        if (!isKeptInMemory(newZoom, viewRect, pageBounds)) {
            recycle();
            return false;
        }

        final boolean childrenRequired = isChildrenRequired(newZoom, viewRect);

        PageTreeNode[] children = page.nodes.getChildren(this);

        if (newZoom < oldZoom) {
            if (LengthUtils.isNotEmpty(children) && !childrenRequired) {
                hasChildren = page.nodes.recycleChildren(this);
                if (isVisible(viewRect, pageBounds) && getBitmap() == null) {
                    decodePageTreeNode(nodesToDecode);
                }
            }
            return true;
        }

        if (childrenRequired) {
            if (LengthUtils.isEmpty(children)) {
                hasChildren = true;
                stopDecodingThisNode("children created");
                children = page.nodes.createChildren(this, childrenZoomThreshold * childrenZoomThreshold);
            }

            for (final PageTreeNode child : children) {
                child.onZoomChanged(oldZoom, newZoom, viewRect, pageBounds, nodesToDecode);
            }

            return true;
        }

        if (getBitmap() == null) {
            decodePageTreeNode(nodesToDecode);
        } else if (!isVisible(viewRect, pageBounds)) {
            if (page.lowMemory) {
                holder.clearDirectRef();
            }
        }
        return true;
    }

    public boolean onPositionChanged(final RectF viewRect, final RectF pageBounds,
            final List<PageTreeNode> nodesToDecode) {
        final float zoom = page.base.getZoomModel().getZoom();

        if (!isKeptInMemory(zoom, viewRect, pageBounds)) {
            recycle();
            return false;
        }

        final boolean childrenRequired = isChildrenRequired(zoom, viewRect);
        PageTreeNode[] children = page.nodes.getChildren(this);

        if (LengthUtils.isNotEmpty(children)) {
            for (final PageTreeNode child : children) {
                child.onPositionChanged(viewRect, pageBounds, nodesToDecode);
            }
            return true;
        }

        if (childrenRequired) {
            hasChildren = true;
            stopDecodingThisNode("children created");
            children = page.nodes.createChildren(this, childrenZoomThreshold * childrenZoomThreshold);
            for (final PageTreeNode child : children) {
                child.onPositionChanged(viewRect, pageBounds, nodesToDecode);
            }
            return true;
        }

        if (getBitmap() == null) {
            decodePageTreeNode(nodesToDecode);
        } else if (!isVisible(viewRect, pageBounds)) {
            if (page.lowMemory) {
                holder.clearDirectRef();
            }
        }
        return true;
    }

    protected void onChildLoaded(final PageTreeNode child, final RectF viewRect, final RectF bounds) {
        if (page.lowMemory) {
            if (page.nodes.isHiddenByChildren(this, viewRect, bounds)) {
                holder.clearDirectRef();
            }
        }
    }

    protected boolean isKeptInMemory(final float zoom, final RectF viewRect, final RectF pageBounds) {
        if (zoom < 2) {
            return page.isKeptInMemory();
        }
        if (zoom < 4) {
            return page.isKeptInMemory() && page.isVisible();
        }

        return this.isVisible(viewRect, pageBounds);
    }

    protected boolean isVisible(final RectF viewRect, final RectF pageBounds) {
        final RectF tr = getTargetRect(viewRect, pageBounds);
        return RectF.intersects(tr, new RectF(0, 0, getBase().getView().getWidth(), getBase().getView().getHeight()));
    }

    protected boolean isChildrenRequired(final float zoom, final RectF viewRect) {
        if (page.nativeResolution) {
            return false;
        }
        if (!page.lowMemory) {
            return zoom > childrenZoomThreshold;
        }

        final Rect rect = page.base.getDecodeService().getScaledSize(page.base.getView().getWidth(),
                page.bounds.width(), page.bounds.height(), pageSliceBounds, zoom);
        final int size = 4 * rect.width() * rect.height();
        return size >= SettingsManager.getAppSettings().getMaxImageSize();
    }

    protected void decodePageTreeNode(final List<PageTreeNode> nodesToDecode) {
        if (setDecodingNow(true)) {
            nodesToDecode.add(this);
        }
    }

    @Override
    public void decodeComplete(final CodecPage codecPage, final Bitmap bitmap) {
        page.base.getView().post(new Runnable() {

            @Override
            public void run() {
                holder.setBitmap(bitmap);
                setDecodingNow(false);
                final IDocumentViewController dc = page.base.getDocumentController();
                final boolean changed = page.setAspectRatio(codecPage);

                if (dc != null) {
                    if (changed) {
                        dc.invalidatePageSizes(InvalidateSizeReason.PAGE_LOADED, page);
                    }
                    final RectF viewRect = dc.getViewRect();
                    final RectF bounds = page.getBounds();
                    if (parent != null) {
                        parent.onChildLoaded(PageTreeNode.this, viewRect, bounds);
                    }
                    if (isVisible(viewRect, bounds)) {
                        dc.redrawView();
                    }
                }
            }
        });
    }

    private boolean setDecodingNow(final boolean decodingNow) {
        if (this.decodingNow.compareAndSet(!decodingNow, decodingNow)) {
            final DecodingProgressModel dpm = page.base.getDecodingProgressModel();
            if (dpm != null) {
                if (decodingNow) {
                    dpm.increase();
                } else {
                    dpm.decrease();
                }
            }
            return true;
        }
        return false;
    }

    private void stopDecodingThisNode(final String reason) {
        if (setDecodingNow(false)) {
            final DecodeService ds = page.base.getDecodeService();
            if (ds != null) {
                ds.stopDecoding(this, reason);
            }
        }
    }

    void draw(final Canvas canvas, final RectF viewRect, final RectF pageBounds, final PagePaint paint) {
        final RectF tr = getTargetRect(viewRect, pageBounds);
        if (!isVisible(viewRect, pageBounds)) {
            return;
        }
        Bitmap bitmap = null;

        // final long start = System.currentTimeMillis();

        if (SettingsManager.getAppSettings().getNightMode()) {
            bitmap = holder.getNightBitmap(tr, paint.getNightBitmapPaint());
        } else {
            bitmap = getBitmap();
        }

        if (bitmap != null) {
            canvas.drawRect(tr, paint.getFillPaint());
            canvas.drawBitmap(bitmap, null, tr, paint.getBitmapPaint());
            // final long end = System.currentTimeMillis();
            // if (LCTX.isDebugEnabled()) {
            // LCTX.d("Draw node: " + page.index.viewIndex + ":" + id + ": [" + bitmap.getWidth() + ", "
            // + bitmap.getHeight() + "] => [" + (int) tr.width() + ", " + (int) tr.height() + "]: "
            // + (end - start) + " ms");
            // }
        }

        drawBrightnessFilter(canvas, tr);

        drawChildren(canvas, viewRect, pageBounds, paint);
    }

    void drawBrightnessFilter(final Canvas canvas, final RectF tr) {
        final int brightness = SettingsManager.getAppSettings().getBrightness();
        if (brightness < 100) {
            final Paint p = new Paint();
            p.setColor(Color.BLACK);
            p.setAlpha(255 - brightness * 255 / 100);
            canvas.drawRect(tr, p);
        }
    }

    void drawChildren(final Canvas canvas, final RectF viewRect, final RectF pageBounds, final PagePaint paint) {
        for (final PageTreeNode child : page.nodes.getChildren(this)) {
            child.draw(canvas, viewRect, pageBounds, paint);
        }
    }

    public RectF getTargetRect(final RectF viewRect, final RectF pageBounds) {
        matrix.reset();

        final RectF bounds = new RectF(pageBounds);
        bounds.offset(-viewRect.left, -viewRect.top);

        matrix.postScale(bounds.width() * page.getTargetRectScale(), bounds.height());
        matrix.postTranslate(bounds.left - bounds.width() * page.getTargetTranslate(), bounds.top);

        final RectF targetRectF = new RectF();
        matrix.mapRect(targetRectF, pageSliceBounds);
        return new RectF(targetRectF);
    }

    public IViewerActivity getBase() {
        return page.base;
    }

    /**
     * Gets the parent node.
     *
     * @return the parent node
     */
    public PageTreeNode getParent() {
        return parent;
    }

    public RectF getPageSliceBounds() {
        return pageSliceBounds;
    }

    public int getPageIndex() {
        return page.index.viewIndex;
    }

    public Bitmap getBitmap() {
        return holder.getBitmap();
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
        buf.append("hasBitmap").append("=").append(getBitmap() != null);

        buf.append("]");
        return buf.toString();
    }

    public int getDocumentPageIndex() {
        return page.index.docIndex;
    }

    private static RectF evaluatePageSliceBounds(final RectF localPageSliceBounds, final PageTreeNode parent) {
        if (parent == null) {
            return localPageSliceBounds;
        }
        final Matrix matrix = new Matrix();
        matrix.postScale(parent.pageSliceBounds.width(), parent.pageSliceBounds.height());
        matrix.postTranslate(parent.pageSliceBounds.left, parent.pageSliceBounds.top);
        final RectF sliceBounds = new RectF();
        matrix.mapRect(sliceBounds, localPageSliceBounds);
        return sliceBounds;
    }

    class BitmapHolder {

        Bitmap bitmap;
        SoftReference<Bitmap> bitmapWeakReference;
        SoftReference<Bitmap> nightWeakReference;

        public Bitmap getBitmap() {
            Bitmap bmp = bitmap;
            if (bmp == null) {
                bmp = bitmapWeakReference != null ? bitmapWeakReference.get() : null;
            }
            if (bmp != null && !bmp.isRecycled()) {
                bitmapWeakReference = new SoftReference<Bitmap>(bmp);
                return bmp;
            } else {
                bitmap = null;
                bitmapWeakReference = null;
            }
            return null;
        }

        public Bitmap getNightBitmap(final RectF targetRect, final Paint paint) {
            Bitmap bmp = nightWeakReference != null ? nightWeakReference.get() : null;
            if (bmp != null && !bmp.isRecycled()) {
                return bmp;
            }
            bmp = getBitmap();
            if (bmp == null || bmp.isRecycled()) {
                return null;
            }
            final Bitmap night = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.RGB_565);
            final Canvas c = new Canvas(night);
            c.drawBitmap(bitmap, 0, 0, paint);
            nightWeakReference = new SoftReference<Bitmap>(night);
            return night;
        }

        public void clearDirectRef() {
            if (bitmap != null) {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("Clear bitmap reference: " + PageTreeNode.this);
                }
                bitmapWeakReference = new SoftReference<Bitmap>(bitmap);
                bitmap = null;
                recycleNightRef();
            }
        }

        public void recycle() {
            if (bitmap != null && !bitmap.isRecycled()) {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("Recycle bitmap reference: " + PageTreeNode.this);
                }
                bitmap.recycle();
            }
            bitmap = null;
            bitmapWeakReference = null;
            recycleNightRef();
        }

        public void setBitmap(final Bitmap bitmap) {
            if (bitmap == null) {
                return;
            }
            if (bitmap.getWidth() == -1 && bitmap.getHeight() == -1) {
                return;
            }
            if (this.bitmap != bitmap) {
                if (this.bitmap != null && !bitmap.isRecycled()) {
                    this.bitmap.recycle();
                }
                this.bitmap = bitmap;
                this.bitmapWeakReference = new SoftReference<Bitmap>(bitmap);
                recycleNightRef();
                page.base.getView().postInvalidate();
            } else {
                this.bitmapWeakReference = new SoftReference<Bitmap>(bitmap);
            }
        }

        void recycleNightRef() {
            final Bitmap night = nightWeakReference != null ? nightWeakReference.get() : null;
            if (night != null) {
                night.recycle();
            }
            nightWeakReference = null;
        }
    }

}
