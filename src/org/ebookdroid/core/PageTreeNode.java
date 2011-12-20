package org.ebookdroid.core;

import org.ebookdroid.core.IDocumentViewController.InvalidateSizeReason;
import org.ebookdroid.core.bitmaps.BitmapManager;
import org.ebookdroid.core.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.crop.PageCropper;
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

    // private static final LogContext LCTX = LogContext.ROOT.lctx("Imaging");

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
                if (viewState.isNodeVisible(this, pageBounds) && getBitmap() == null) {
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
        } else if (getBitmap() == null) {
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

        if (getBitmap() == null) {
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
        if (viewState.decodeMode == DecodeMode.NATIVE_RESOLUTION) {
            return false;
        }

        final Rect rect = page.base.getDecodeService().getScaledSize(viewState,
                page.bounds.width() * page.getTargetRectScale(), page.bounds.height(),
                croppedBounds != null ? croppedBounds : pageSliceBounds, page.getTargetRectScale(),
                getSliceGeneration());

        System.out.println("isRequired(" + getSliceGeneration() + "): " + rect);

        if (viewState.decodeMode == DecodeMode.NORMAL) {
            // We need to check for 2048 for HW accel. limitations.
            return (viewState.zoom > childrenZoomThreshold) || (rect.width() > 2048) || (rect.height() > 2048);
        }

        final long size = BitmapManager.getBitmapBufferSize(getBitmap(), rect);
        return size >= SettingsManager.getAppSettings().getMaxImageSize();
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

        System.out.println("decodeComplete:" + this.page.index + ", " + bitmap + ", bounds: " + bitmapBounds);
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
        page.base.getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                holder.setBitmap(bitmap, bitmapBounds);
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

        final Bitmap bitmap = getBitmap(viewState, paint);

        if (!allChildrenHasBitmap(viewState, paint)) {
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, holder.getBitmapBounds(), tr, paint.bitmapPaint);
            }

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

    private Bitmap getBitmap(final ViewState viewState, final PagePaint paint) {
        return viewState.nightMode ? holder.getNightBitmap(paint.nightBitmapPaint) : holder.getBitmap();
    }

    boolean hasBitmap(final ViewState viewState, final PagePaint paint) {
        return getBitmap(viewState, paint) != null;
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

    class BitmapHolder {

        BitmapRef bitmap;
        BitmapRef night;
        Rect bounds;

        public synchronized Bitmap getBitmap() {
            final Bitmap bmp = bitmap != null ? bitmap.getBitmap() : null;
            if (bmp == null || bmp.isRecycled()) {
                if (bitmap != null) {
                    BitmapManager.release(bitmap);
                    bitmap = null;
                }
            }
            return bmp;
        }

        public synchronized Rect getBitmapBounds() {
            return bounds;
        }

        public synchronized Bitmap getNightBitmap(final Paint paint) {
            Bitmap bmp = null;
            if (night != null) {
                bmp = night.getBitmap();
                if (bmp == null || bmp.isRecycled()) {
                    BitmapManager.release(night);
                    night = null;
                }
                return bmp;
            }
            bmp = getBitmap();
            if (bmp == null || bmp.isRecycled()) {
                return null;
            }
            this.night = BitmapManager.getBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.RGB_565);
            final Canvas c = new Canvas(night.getBitmap());
            c.drawRect(0, 0, bmp.getWidth(), bmp.getHeight(), PagePaint.NIGHT.fillPaint);
            c.drawBitmap(bmp, 0, 0, paint);

            bitmap.clearDirectRef();
            return night.getBitmap();
        }

        public synchronized void clearDirectRef(final List<BitmapRef> bitmapsToRecycle) {
            if (bitmap != null) {
                bitmap.clearDirectRef();
            }
            if (night != null) {
                night.clearDirectRef();
            }
        }

        public synchronized void recycle(final List<BitmapRef> bitmapsToRecycle) {
            if (bitmap != null) {
                if (bitmapsToRecycle != null) {
                    bitmapsToRecycle.add(bitmap);
                } else {
                    BitmapManager.release(bitmap);
                }
                bitmap = null;
            }
            if (night != null) {
                if (bitmapsToRecycle != null) {
                    bitmapsToRecycle.add(night);
                } else {
                    BitmapManager.release(night);
                }
                night = null;
            }
        }

        public synchronized void setBitmap(final BitmapRef ref, final Rect bitmapBounds) {
            if (ref == null) {
                return;
            }

            this.bounds = bitmapBounds;
            final List<BitmapRef> bitmapsToRecycle = new ArrayList<BitmapRef>(2);
            recycle(bitmapsToRecycle);
            BitmapManager.release(bitmapsToRecycle);

            this.bitmap = ref;
        }
    }

}
