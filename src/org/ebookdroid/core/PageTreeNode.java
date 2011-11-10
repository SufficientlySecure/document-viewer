package org.ebookdroid.core;

import org.ebookdroid.core.IDocumentViewController.InvalidateSizeReason;
import org.ebookdroid.core.bitmaps.BitmapManager;
import org.ebookdroid.core.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.CodecPage;
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

    public boolean onZoomChanged(final float oldZoom, final ViewState viewState, boolean committed,
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

        if (viewState.decodeMode == DecodeMode.NORMAL) {
            return viewState.zoom > childrenZoomThreshold;
        }

        final Rect rect = page.base.getDecodeService().getScaledSize(viewState.realRect.width(), page.bounds.width(),
                page.bounds.height(), pageSliceBounds, viewState.zoom, page.getTargetRectScale());

        final long size = BitmapManager.getBitmapBufferSize(getBitmap(), rect);
        return size >= SettingsManager.getAppSettings().getMaxImageSize();
    }

    protected void decodePageTreeNode(final List<PageTreeNode> nodesToDecode, ViewState viewState) {
        if (setDecodingNow(true)) {
            bitmapZoom = viewState.zoom;
            nodesToDecode.add(this);
        }
    }

    @Override
    public void decodeComplete(final CodecPage codecPage, final BitmapRef bitmap, final Rect bitmapBounds) {

        if (bitmap == null || bitmapBounds == null) {
            return;
        }
        
        if (SettingsManager.getBookSettings().cropPages) {
            if (id == 0 && !cropped) {
                float avgLum = calculateAvgLum(bitmap, bitmapBounds);
                float left = getLeftBound(bitmap, bitmapBounds, avgLum);
                float right = getRightBound(bitmap, bitmapBounds, avgLum);
                float top = getTopBound(bitmap, bitmapBounds, avgLum);
                float bottom = getBottomBound(bitmap, bitmapBounds, avgLum);
    
                croppedBounds = new RectF(left * pageSliceBounds.width() + pageSliceBounds.left, top
                        * pageSliceBounds.height() + pageSliceBounds.top, right * pageSliceBounds.width()
                        + pageSliceBounds.left, bottom * pageSliceBounds.height() + pageSliceBounds.top);
                cropped = true;
    
                BitmapManager.release(bitmap);
                
                page.base.getActivity().runOnUiThread(new Runnable() {
    
                    @Override
                    public void run() {
                        setDecodingNow(false);
                        page.base.getDecodeService().decodePage(new ViewState(PageTreeNode.this), PageTreeNode.this,
                                croppedBounds);
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

                final IDocumentViewController dc = page.base.getDocumentController();
                final DocumentModel dm = page.base.getDocumentModel();

                if (dc != null && dm != null) {
                    final boolean changed = page.setAspectRatio(bitmapBounds.width(), bitmapBounds.height());

                    ViewState viewState = new ViewState(dc);
                    if (changed) {
                        dc.invalidatePageSizes(InvalidateSizeReason.PAGE_LOADED, page);
                        viewState = dc.updatePageVisibility(page.index.viewIndex, 0, viewState.zoom);
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

    private float getLeftBound(BitmapRef bitmap, Rect bitmapBounds, float avgLum) {
        Bitmap bmp = bitmap.getBitmap();
        final int w = bmp.getWidth() / 3;
        int whiteCount = 0;
        int x = 0;
        int lineSize = 5;
        for (x = bitmapBounds.left; x < bitmapBounds.left + w; x += lineSize) {
            boolean white = isRectWhite(bmp, x, bitmapBounds.top + 20, x + lineSize, bitmapBounds.bottom - 20, avgLum);
            if (white) {
                whiteCount++;
            } else {
                if (whiteCount >= 1) {
                    return (float) (x - bitmapBounds.left) / bitmapBounds.width();
                }
                whiteCount = 0;
            }
        }
        return whiteCount > 0 ? (float) (x - bitmapBounds.left) / bitmapBounds.width() : 0;
    }

    private float getTopBound(BitmapRef bitmap, Rect bitmapBounds, float avgLum) {
        Bitmap bmp = bitmap.getBitmap();
        final int h = bmp.getHeight() / 3;
        int whiteCount = 0;
        int y = 0;
        int lineSize = 5;
        for (y = bitmapBounds.top; y < bitmapBounds.top + h; y += lineSize) {
            boolean white = isRectWhite(bmp, bitmapBounds.left + 20, y, bitmapBounds.right - 20, y + lineSize, avgLum);
            if (white) {
                whiteCount++;
            } else {
                if (whiteCount >= 1) {
                    return (float) (y - bitmapBounds.top) / bitmapBounds.height();
                }
                whiteCount = 0;
            }
        }
        return whiteCount > 0 ? (float) (y - bitmapBounds.top) / bitmapBounds.height() : 0;
    }

    private float getRightBound(BitmapRef bitmap, Rect bitmapBounds, float avgLum) {
        Bitmap bmp = bitmap.getBitmap();
        final int w = bmp.getWidth() / 3;
        int whiteCount = 0;
        int x = 0;
        int lineSize = 5;
        for (x = bitmapBounds.right - lineSize; x > bitmapBounds.right - w; x -= lineSize) {
            boolean white = isRectWhite(bmp, x, bitmapBounds.top + 20, x + lineSize, bitmapBounds.bottom - 20, avgLum);
            if (white) {
                whiteCount++;
            } else {
                if (whiteCount >= 1) {
                    return (float) (x + lineSize - bitmapBounds.left) / bitmapBounds.width();
                }
                whiteCount = 0;
            }
        }
        return whiteCount > 0 ? (float) (x + lineSize - bitmapBounds.left) / bitmapBounds.width() : 1;
    }

    private float getBottomBound(BitmapRef bitmap, Rect bitmapBounds, float avgLum) {
        Bitmap bmp = bitmap.getBitmap();
        final int h = bmp.getHeight() / 3;
        int whiteCount = 0;
        int y = 0;
        int lineSize = 5;
        for (y = bitmapBounds.bottom - lineSize; y > bitmapBounds.bottom - h; y -= lineSize) {
            boolean white = isRectWhite(bmp, bitmapBounds.left + 20, y, bitmapBounds.right - 20, y + lineSize, avgLum);
            if (white) {
                whiteCount++;
            } else {
                if (whiteCount >= 1) {
                    return (float) (y + lineSize - bitmapBounds.top) / bitmapBounds.height();
                }
                whiteCount = 0;
            }
        }
        return whiteCount > 0 ? (float) (y + lineSize - bitmapBounds.top) / bitmapBounds.height() : 1;
    }

    private boolean isRectWhite(Bitmap bmp, int l, int t, int r, int b, float avgLum) {
        for (int x = l; x < r; x++) {
            for (int y = t; y < b; y++) {
                int c = bmp.getPixel(x, y);

                float lum = getLum(c);
                if ((lum < avgLum) && ((avgLum - lum) * 10 > avgLum)) {
                    return false;
                }

            }
        }
        return true;
    }

    private float calculateAvgLum(BitmapRef bitmap, Rect bitmapBounds) {
        Bitmap bmp = bitmap.getBitmap();
        if (bmp == null) {
            return 1000;
        }
        float lum = 0f;
        int size = 20;
        int count = 0;
        for (int x = bitmapBounds.left + bitmapBounds.width() / 2 - size; x < bitmapBounds.left + bitmapBounds.width()
                / 2 + size; x++) {
            for (int y = bitmapBounds.top + bitmapBounds.height() / 2 - size; y < bitmapBounds.top
                    + bitmapBounds.height() / 2 + size; y++) {
                int c = bmp.getPixel(x, y);

                lum += getLum(c);
                count++;
            }
        }

        return lum / (count);
    }

    private float getLum(int c) {
        int r = (c & 0xFF0000) >> 16;
        int g = (c & 0xFF00) >> 8;
        int b = c & 0xFF;
        /*
         * return (0.3f * r + 0.59f * g + 0.11f * b);
         */
        int min = Math.min(r, Math.min(g, b));
        int max = Math.max(r, Math.max(g, b));
        return (min + max) / 2;
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

        final Bitmap bitmap = viewState.nightMode ? holder.getNightBitmap(paint.nightBitmapPaint) : holder.getBitmap();

        if (bitmap != null) {
            canvas.drawBitmap(bitmap, holder.getBitmapBounds(), tr, paint.bitmapPaint);
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

    public RectF getTargetCroppedRect(final RectF viewRect, final RectF pageBounds) {
        matrix.reset();

        final RectF bounds = new RectF(pageBounds);
        bounds.offset(-viewRect.left, -viewRect.top);

        matrix.postScale(bounds.width() * page.getTargetRectScale(), bounds.height());
        matrix.postTranslate(bounds.left - bounds.width() * page.getTargetTranslate(), bounds.top);

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
