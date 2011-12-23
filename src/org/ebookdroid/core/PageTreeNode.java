package org.ebookdroid.core;

import org.ebookdroid.core.IDocumentViewController.InvalidateSizeReason;
import org.ebookdroid.core.bitmaps.BitmapManager;
import org.ebookdroid.core.bitmaps.BitmapRef;
import org.ebookdroid.core.bitmaps.Bitmaps;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.crop.PageCropper;
import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.models.DecodingProgressModel;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.utils.LengthUtils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PageTreeNode implements DecodeService.DecodeCallback {

    private static final LogContext LCTX = LogContext.ROOT.lctx("Page");

    final Page page;
    final PageTreeNode parent;
    final long id;
    final String shortId;
    final AtomicBoolean decodingNow = new AtomicBoolean();
    final BitmapHolder holder = new BitmapHolder();

    final float childrenZoomThreshold;
    final RectF pageSliceBounds;
    final Matrix matrix = new Matrix();

    float bitmapZoom = 1;
    boolean hasChildren = false;
    private boolean cropped;
    RectF croppedBounds = null;

    PageTreeNode(final Page page, final PageTreeNode parent, final long id, final RectF localPageSliceBounds,
            final float childrenZoomThreshold) {
        this.id = id;
        this.shortId = page.index.viewIndex + ":" + id;
        this.parent = parent;
        this.pageSliceBounds = evaluatePageSliceBounds(localPageSliceBounds, parent);
        this.croppedBounds = evaluateCroppedPageSliceBounds(localPageSliceBounds, parent);
        this.page = page;
        this.childrenZoomThreshold = childrenZoomThreshold;
    }

    public void recycle(final List<BitmapRef> bitmapsToRecycle) {
        stopDecodingThisNode("node recycling");
        holder.recycle(bitmapsToRecycle);
        hasChildren = page.nodes.recycleChildren(this, bitmapsToRecycle);
    }

    public boolean onZoomChanged(final float oldZoom, final ViewState viewState, final boolean committed,
            final RectF pageBounds, final List<PageTreeNode> nodesToDecode, final List<BitmapRef> bitmapsToRecycle) {
        if (!viewState.isNodeKeptInMemory(this, pageBounds)) {
            recycle(bitmapsToRecycle);
            return false;
        }

        final boolean childrenRequired = isChildrenRequired(viewState);

        PageTreeNode[] children = page.nodes.getChildren(this);

        if (viewState.zoom < oldZoom) {
            if (!childrenRequired) {
                if (LengthUtils.isNotEmpty(children)) {
                    hasChildren = page.nodes.recycleChildren(this, bitmapsToRecycle);
                }
                if (viewState.isNodeVisible(this, pageBounds) && holder.getBitmap() == null) {
                    decodePageTreeNode(nodesToDecode, viewState);
                }
            }
            return true;
        }

        if (childrenRequired) {
            if (LengthUtils.isEmpty(children)) {
                hasChildren = true;
                if (id != 0 || viewState.decodeMode == DecodeMode.LOW_MEMORY) {
                    stopDecodingThisNode("children created");
                }
                children = page.nodes.createChildren(this, calculateChildThreshold());
            }

            for (final PageTreeNode child : children) {
                child.onZoomChanged(oldZoom, viewState, committed, pageBounds, nodesToDecode, bitmapsToRecycle);
            }

            return true;
        }

        if (isReDecodingRequired(committed, viewState)) {
            stopDecodingThisNode("Zoom changed");
            decodePageTreeNode(nodesToDecode, viewState);
        } else if (holder.getBitmap() == null) {
            decodePageTreeNode(nodesToDecode, viewState);
        }
        return true;
    }

    private boolean isReDecodingRequired(final boolean committed, final ViewState viewState) {
        return (committed && viewState.zoom != bitmapZoom) || viewState.zoom > 1.2 * bitmapZoom;
    }

    protected float calculateChildThreshold() {
        return childrenZoomThreshold * childrenZoomThreshold;
    }

    public boolean onPositionChanged(final ViewState viewState, final RectF pageBounds,
            final List<PageTreeNode> nodesToDecode, final List<BitmapRef> bitmapsToRecycle) {

        if (!viewState.isNodeKeptInMemory(this, pageBounds)) {
            recycle(bitmapsToRecycle);
            return false;
        }

        final boolean childrenRequired = isChildrenRequired(viewState);
        PageTreeNode[] children = page.nodes.getChildren(this);

        if (LengthUtils.isNotEmpty(children)) {
            for (final PageTreeNode child : children) {
                child.onPositionChanged(viewState, pageBounds, nodesToDecode, bitmapsToRecycle);
            }
            return true;
        }

        if (childrenRequired) {
            hasChildren = true;
            if (id != 0 || viewState.decodeMode == DecodeMode.LOW_MEMORY) {
                stopDecodingThisNode("children created");
            }
            children = page.nodes.createChildren(this, calculateChildThreshold());
            for (final PageTreeNode child : children) {
                child.onPositionChanged(viewState, pageBounds, nodesToDecode, bitmapsToRecycle);
            }
            return true;
        }

        if (holder.getBitmap() == null) {
            decodePageTreeNode(nodesToDecode, viewState);
        }
        return true;
    }

    protected void onChildLoaded(final PageTreeNode child, final ViewState viewState, final RectF bounds,
            final List<BitmapRef> bitmapsToRecycle) {
        if (viewState.decodeMode == DecodeMode.LOW_MEMORY) {
            if (page.nodes.isHiddenByChildren(this, viewState, bounds)) {
                holder.clearDirectRef(bitmapsToRecycle);
            }
        }
    }

    protected boolean isChildrenRequired(final ViewState viewState) {
        if (viewState.decodeMode == DecodeMode.NORMAL && viewState.zoom > childrenZoomThreshold) {
            return true;
        }

        final DecodeService ds = page.base.getDecodeService();
        if (ds == null) {
            return false;
        }

        long memoryLimit = ds.getMemoryLimit();
        if (viewState.decodeMode == DecodeMode.LOW_MEMORY) {
            memoryLimit = Math.min(memoryLimit, SettingsManager.getAppSettings().getMaxImageSize());
        }

        final Rect rect = getActualRect(viewState, ds);

        final long size = BitmapManager.getBitmapBufferSize(rect.width(), rect.height(), ds.getBitmapConfig());

        LCTX.d(getFullId() + ".isChildrenRequired(): rect=" + rect + ", size=" + size + ", limit=" + memoryLimit);

        final boolean textureSizeExceedeed = (rect.width() > 2048) || (rect.height() > 2048);
        final boolean memoryLimitExceeded = size + 4096 >= memoryLimit;

        return textureSizeExceedeed || memoryLimitExceeded;
    }

    private Rect getActualRect(final ViewState viewState, final DecodeService ds) {
        final RectF actual = croppedBounds != null ? croppedBounds : pageSliceBounds;
        final float widthScale = page.getTargetRectScale();
        final float pageWidth = page.bounds.width() * widthScale;

        if (viewState.decodeMode == DecodeMode.NATIVE_RESOLUTION) {
            return ds.getNativeSize(pageWidth, page.bounds.height(), actual, widthScale);
        }

        return ds.getScaledSize(viewState, pageWidth, page.bounds.height(), actual, widthScale, getSliceGeneration());
    }

    public int getSliceGeneration() {
        return (int) (parent != null ? parent.childrenZoomThreshold : 1);
    }

    protected void decodePageTreeNode(final List<PageTreeNode> nodesToDecode, final ViewState viewState) {
        if (setDecodingNow(true)) {
            bitmapZoom = viewState.zoom;
            nodesToDecode.add(this);
        }
    }

    @Override
    public void decodeComplete(final CodecPage codecPage, final BitmapRef bitmap, final Rect bitmapBounds) {

        if (bitmap == null || bitmapBounds == null) {
            page.base.getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    setDecodingNow(false);
                }
            });
            return;
        }

        if (SettingsManager.getBookSettings().cropPages) {
            if (id == 0 && !cropped) {
                croppedBounds = PageCropper.getCropBounds(bitmap, bitmapBounds, pageSliceBounds);
                cropped = true;

                BitmapManager.release(bitmap);

                page.base.getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        setDecodingNow(false);
                        final DecodeService decodeService = page.base.getDecodeService();
                        if (decodeService != null) {
                            decodeService
                                    .decodePage(new ViewState(PageTreeNode.this), PageTreeNode.this, croppedBounds);
                        }
                    }
                });
                return;
            }
        }

        final Bitmaps bitmaps = new Bitmaps(bitmap, bitmapBounds);
        BitmapManager.release(bitmap);

        page.base.getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                holder.setBitmap(bitmaps);
                setDecodingNow(false);

                page.base.getDocumentController().pageUpdated(page.index.viewIndex);
                final IDocumentViewController dc = page.base.getDocumentController();
                final DocumentModel dm = page.base.getDocumentModel();

                if (dc != null && dm != null) {
                    final boolean changed = page.setAspectRatio(bitmapBounds.width(), bitmapBounds.height());

                    ViewState viewState = new ViewState(dc);
                    if (changed) {
                        dc.invalidatePageSizes(InvalidateSizeReason.PAGE_LOADED, page);
                        viewState = dc.updatePageVisibility(dm.getCurrentViewPageIndex(), 0, viewState.zoom);
                    }
                    final RectF bounds = viewState.getBounds(page);
                    if (parent != null) {
                        final List<BitmapRef> bitmapsToRecycle = new ArrayList<BitmapRef>(2);
                        parent.onChildLoaded(PageTreeNode.this, viewState, bounds, bitmapsToRecycle);
                        BitmapManager.release(bitmapsToRecycle);
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

        if (!allChildrenHasBitmap(viewState, paint)) {
            holder.drawBitmap(viewState, canvas, paint, tr);

            drawBrightnessFilter(canvas, tr);
        }
        drawChildren(canvas, viewState, pageBounds, paint);
    }

    private boolean allChildrenHasBitmap(final ViewState viewState, final PagePaint paint) {
        for (final PageTreeNode child : page.nodes.getChildren(this)) {
            if (!child.hasBitmap(viewState, paint)) {
                return false;
            }
        }
        return page.nodes.getChildren(this).length > 0;
    }

    boolean hasBitmap(final ViewState viewState, final PagePaint paint) {
        return holder.hasBitmap(viewState, paint);
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

        matrix.postScale(pageBounds.width() * page.getTargetRectScale(), pageBounds.height());
        matrix.postTranslate(pageBounds.left - pageBounds.width() * page.getTargetTranslate(), pageBounds.top);

        final RectF targetRectF = new RectF();
        matrix.mapRect(targetRectF, pageSliceBounds);
        return new RectF(targetRectF);
    }

    public RectF getTargetCroppedRect(final RectF viewRect, final RectF pageBounds) {
        matrix.reset();

        matrix.postScale(pageBounds.width() * page.getTargetRectScale(), pageBounds.height());
        matrix.postTranslate(pageBounds.left - pageBounds.width() * page.getTargetTranslate(), pageBounds.top);

        final RectF targetRectF = new RectF();
        matrix.mapRect(targetRectF, croppedBounds);
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

    public boolean hasBitmap() {
        return holder.getBitmap() != null;
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
        buf.append("hasBitmap").append("=").append(holder.getBitmap() != null);

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

    private static RectF evaluateCroppedPageSliceBounds(final RectF localPageSliceBounds, final PageTreeNode parent) {
        if (parent == null) {
            return null;
        }
        if (parent.croppedBounds == null) {
            return null;
        }
        final Matrix matrix = new Matrix();
        matrix.postScale(parent.croppedBounds.width(), parent.croppedBounds.height());
        matrix.postTranslate(parent.croppedBounds.left, parent.croppedBounds.top);
        final RectF sliceBounds = new RectF();
        matrix.mapRect(sliceBounds, localPageSliceBounds);
        return sliceBounds;
    }

    static class BitmapHolder {

        Bitmaps day;
        Bitmaps night;

        public synchronized void drawBitmap(final ViewState viewState, final Canvas canvas, final PagePaint paint,
                final RectF tr) {

            if (viewState.nightMode) {
                if (null != getNightBitmap(paint.nightBitmapPaint)) {
                    night.draw(viewState, canvas, paint, tr);
                }
            } else {
                if (day != null) {
                    day.draw(viewState, canvas, paint, tr);
                }
            }
        }

        public boolean hasBitmap(final ViewState viewState, final PagePaint paint) {
            return getBitmap(viewState, paint) != null;
        }

        public Bitmap[] getBitmap(final ViewState viewState, final PagePaint paint) {
            return viewState.nightMode ? getNightBitmap(paint.nightBitmapPaint) : getBitmap();
        }

        public synchronized Bitmap[] getBitmap() {
            return day != null ? day.getBitmaps() : null;
        }

        public synchronized Bitmap[] getNightBitmap(final Paint paint) {
            Bitmap[] res = null;
            if (night == null) {
                final Bitmap[] days = day != null ? day.getBitmaps() : null;
                if (days == null) {
                    return null;
                }
                night = new Bitmaps(day, days, paint);
            }
            if (night != null) {
                res = night.getBitmaps();
                if (res != null) {
                    return res;
                }
                night = null;
            }
            return res;
        }

        public synchronized void clearDirectRef(final List<BitmapRef> bitmapsToRecycle) {
        }

        public synchronized void recycle(final List<BitmapRef> bitmapsToRecycle) {
            if (day != null) {
                day.recycle();
                day = null;
            }
            if (night != null) {
                night.recycle();
                night = null;
            }
        }

        public synchronized void setBitmap(final Bitmaps bitmaps) {
            if (bitmaps == null) {
                return;
            }

            recycle(null);

            this.day = bitmaps;
        }
    }

}
