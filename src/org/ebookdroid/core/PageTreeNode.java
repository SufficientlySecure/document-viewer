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
    final String shortId;
    final AtomicBoolean decodingNow = new AtomicBoolean();
    final BitmapHolder holder = new BitmapHolder();

    final float childrenZoomThreshold;
    final RectF pageSliceBounds;
    final Matrix matrix = new Matrix();

    boolean hasChildren = false;

    PageTreeNode(final Page page, final PageTreeNode parent, final long id, final RectF localPageSliceBounds,
            final float childrenZoomThreshold) {
        this.id = id;
        this.shortId = page.index.viewIndex + ":" + id;
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

    public boolean onZoomChanged(final float oldZoom, final ViewState viewState, final RectF pageBounds,
            final List<PageTreeNode> nodesToDecode) {
        if (!isKeptInMemory(viewState, pageBounds)) {
            recycle();
            return false;
        }

        final boolean childrenRequired = isChildrenRequired(viewState);

        PageTreeNode[] children = page.nodes.getChildren(this);

        if (viewState.zoom < oldZoom) {
            if (LengthUtils.isNotEmpty(children) && !childrenRequired) {
                hasChildren = page.nodes.recycleChildren(this);
                if (viewState.isNodeVisible(this, pageBounds) && getBitmap() == null) {
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
                child.onZoomChanged(oldZoom, viewState, pageBounds, nodesToDecode);
            }

            return true;
        }

        if (getBitmap() == null) {
            decodePageTreeNode(nodesToDecode);
        } else if (!viewState.isNodeVisible(this, pageBounds)) {
            if (viewState.lowMemory) {
                holder.clearDirectRef();
            }
        }
        return true;
    }

    public boolean onPositionChanged(final ViewState viewState, final RectF pageBounds,
            final List<PageTreeNode> nodesToDecode) {

        if (!isKeptInMemory(viewState, pageBounds)) {
            recycle();
            return false;
        }

        final boolean childrenRequired = isChildrenRequired(viewState);
        PageTreeNode[] children = page.nodes.getChildren(this);

        if (LengthUtils.isNotEmpty(children)) {
            for (final PageTreeNode child : children) {
                child.onPositionChanged(viewState, pageBounds, nodesToDecode);
            }
            return true;
        }

        if (childrenRequired) {
            hasChildren = true;
            stopDecodingThisNode("children created");
            children = page.nodes.createChildren(this, childrenZoomThreshold * childrenZoomThreshold);
            for (final PageTreeNode child : children) {
                child.onPositionChanged(viewState, pageBounds, nodesToDecode);
            }
            return true;
        }

        if (getBitmap() == null) {
            decodePageTreeNode(nodesToDecode);
        } else if (!viewState.isNodeVisible(this, pageBounds)) {
            if (viewState.lowMemory) {
                holder.clearDirectRef();
            }
        }
        return true;
    }

    protected void onChildLoaded(final PageTreeNode child, final ViewState viewState, final RectF bounds) {
        if (viewState.lowMemory) {
            if (page.nodes.isHiddenByChildren(this, viewState, bounds)) {
                holder.clearDirectRef();
            }
        }
    }

    protected boolean isKeptInMemory(final ViewState viewState, final RectF pageBounds) {
        if (viewState.zoom < 2) {
            return viewState.isPageKeptInMemory(page) || viewState.isPageVisible(page);
        }
        if (viewState.zoom < 4) {
            return viewState.isPageKeptInMemory(page) && viewState.isPageVisible(page);
        }
        return viewState.isPageVisible(page) && viewState.isNodeVisible(this, pageBounds);
    }

    protected boolean isChildrenRequired(final ViewState viewState) {
        if (viewState.nativeResolution) {
            return false;
        }
        if (!viewState.lowMemory) {
            return viewState.zoom > childrenZoomThreshold;
        }

        final Rect rect = page.base.getDecodeService().getScaledSize(viewState.realRect.width(), page.bounds.width(),
                page.bounds.height(), pageSliceBounds, viewState.zoom);
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
                    final ViewState viewState = new ViewState(dc);
                    if (changed) {
                        dc.invalidatePageSizes(InvalidateSizeReason.PAGE_LOADED, page);
                    }
                    final RectF bounds = viewState.getBounds(page);
                    if (parent != null) {
                        parent.onChildLoaded(PageTreeNode.this, viewState, bounds);
                    }
                    if (viewState.isNodeVisible(PageTreeNode.this, bounds)) {
                        dc.redrawView(viewState);
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

    void draw(final Canvas canvas, final ViewState viewState, final RectF pageBounds, final PagePaint paint) {
        final RectF tr = getTargetRect(viewState.viewRect, pageBounds);
        if (!viewState.isNodeVisible(this, pageBounds)) {
            return;
        }
        Bitmap bitmap = null;

        if (SettingsManager.getAppSettings().getNightMode()) {
            bitmap = holder.getNightBitmap(tr, paint.nightBitmapPaint);
        } else {
            bitmap = getBitmap();
        }

        if (bitmap != null) {
            canvas.drawBitmap(bitmap, null, tr, paint.bitmapPaint);
        } else if (decodingNow.get()) {
            canvas.drawRect(tr, paint.decodingPaint);
        }

        drawBrightnessFilter(canvas, tr);

        drawChildren(canvas, viewState, pageBounds, paint);
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

    void drawChildren(final Canvas canvas, final ViewState viewState, final RectF pageBounds, final PagePaint paint) {
        for (final PageTreeNode child : page.nodes.getChildren(this)) {
            child.draw(canvas, viewState, pageBounds, paint);
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

    public String getFullId() {
        return page.index + ":" + id;
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

        public synchronized Bitmap getBitmap() {
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

        public synchronized Bitmap getNightBitmap(final RectF targetRect, final Paint paint) {
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
            c.drawBitmap(bmp, 0, 0, paint);
            nightWeakReference = new SoftReference<Bitmap>(night);
            return night;
        }

        public synchronized void clearDirectRef() {
            if (bitmap != null) {
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("Clear bitmap reference: " + PageTreeNode.this);
                }
                bitmapWeakReference = new SoftReference<Bitmap>(bitmap);
                bitmap = null;
                recycleNightRef();
            }
        }

        public synchronized void recycle() {
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

        public synchronized void setBitmap(final Bitmap bitmap) {
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

        synchronized void recycleNightRef() {
            final Bitmap night = nightWeakReference != null ? nightWeakReference.get() : null;
            if (night != null) {
                night.recycle();
            }
            nightWeakReference = null;
        }
    }

}
