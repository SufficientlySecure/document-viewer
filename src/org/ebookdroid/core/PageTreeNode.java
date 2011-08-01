package org.ebookdroid.core;

import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicBoolean;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;

public class PageTreeNode implements DecodeService.DecodeCallback {
    //private static final int SLICE_SIZE = 65535;
	private static final int SLICE_SIZE = 131070;
    private Bitmap bitmap;
    private SoftReference<Bitmap> bitmapWeakReference;
    private final AtomicBoolean decodingNow = new AtomicBoolean();
    private final RectF pageSliceBounds;
    private final Page page;
    private final IViewerActivity base;
    private PageTreeNode[] children;
    private final float childrenZoomThreshold;
    private Matrix matrix = new Matrix();
    private boolean invalidateFlag;
    private Rect targetRect;
    private RectF targetRectF;
    private final boolean slice_limit;
    private final PageTreeNode parent;

    PageTreeNode(IViewerActivity base, RectF localPageSliceBounds, Page page, float childrenZoomThreshold, PageTreeNode parent, boolean sliceLimit) {
        this.base = base;
        this.parent = parent;
        this.pageSliceBounds = evaluatePageSliceBounds(localPageSliceBounds, parent);
        this.page = page;
        this.childrenZoomThreshold = childrenZoomThreshold;
        this.slice_limit = sliceLimit;
    }

    public IViewerActivity getBase() {
        return base;
    }

    /**
     * Gets the parent node.
     *
     * @return the parent node
     */
    public PageTreeNode getParent() {
        return parent;
    }


    public RectF getPageSliceBounds()
    {
        return pageSliceBounds;
    }

    public void updateVisibility() {
        invalidateChildren();
        if (children != null) {
            for (PageTreeNode child : children) {
                child.updateVisibility();
            }
        }
        if (isVisible()) {
            if (!thresholdHit()) {
                if (getBitmap() != null && !invalidateFlag) {
                    restoreBitmapReference();
                } else {
                    decodePageTreeNode();
                }
            }
        }
        if (!isVisibleAndNotHiddenByChildren()) {
            stopDecodingThisNode("node hidden");
            setBitmap(null);
        }
    }

    public void invalidate() {
        invalidateChildren();
        invalidateRecursive();
        updateVisibility();
    }

    private void invalidateRecursive() {
        invalidateFlag = true;
        if (children != null) {
            for (PageTreeNode child : children) {
                child.invalidateRecursive();
            }
        }
        // stopDecodingThisNode("node invalidation");
    }

    void invalidateNodeBounds() {
        targetRect = null;
        targetRectF = null;
        if (children != null) {
            for (PageTreeNode child : children) {
                child.invalidateNodeBounds();
            }
        }
    }


    void draw(Canvas canvas, PagePaint paint) {
        if (getBitmap() != null) {
        	canvas.drawRect(getTargetRect(), paint.getFillPaint());
            canvas.drawBitmap(getBitmap(), null, getTargetRect(), paint.getBitmapPaint());
        }
        if (children == null) {
            return;
        }
        for (PageTreeNode child : children) {
            child.draw(canvas, paint);
        }
    }

    private boolean isVisible() {
        boolean pageTreeNodeVisible = base.getDocumentController().isPageTreeNodeVisible(this);
        //Log.d("DocModel", "Node visibility: " + this + " -> " + pageTreeNodeVisible);
        return pageTreeNodeVisible;
    }

    public RectF getTargetRectF() {
        if (targetRectF == null) {
            targetRectF = new RectF(getTargetRect());
        }
        return targetRectF;
    }

    private void invalidateChildren() {
        if (thresholdHit() && children == null && isVisible()) {
        	final float newThreshold = childrenZoomThreshold * 2;
            children = new PageTreeNode[]
                    {
                            new PageTreeNode(base, new RectF(0, 0, 0.5f, 0.5f), page, newThreshold, this, slice_limit),
                            new PageTreeNode(base, new RectF(0.5f, 0, 1.0f, 0.5f), page, newThreshold, this, slice_limit),
                            new PageTreeNode(base, new RectF(0, 0.5f, 0.5f, 1.0f), page, newThreshold, this, slice_limit),
                            new PageTreeNode(base, new RectF(0.5f, 0.5f, 1.0f, 1.0f), page, newThreshold, this, slice_limit)
                    };

        }
        if (!thresholdHit() && getBitmap() != null || !isVisible()) {
            recycleChildren();
        }
    }

    private boolean thresholdHit() {
    	if (slice_limit)
    	{
    		float zoom = base.getZoomModel().getZoom();
    		int mainWidth = base.getDocumentController().getView().getWidth();
    		float height = page.getPageHeight(mainWidth, zoom);
    		return (mainWidth * zoom * height) / (childrenZoomThreshold * childrenZoomThreshold) > SLICE_SIZE;
    	} else {
    	    return base.getZoomModel().getZoom() > childrenZoomThreshold;
    	}
    }

    public Bitmap getBitmap() {
        return bitmapWeakReference != null ? bitmapWeakReference.get() : null;
    }

    private void restoreBitmapReference() {
        setBitmap(getBitmap());
    }

    private void decodePageTreeNode()
    {
        if (setDecodingNow(true))
        {
            int width = base.getView().getWidth();
            float zoom = base.getZoomModel().getZoom() * page.getTargetRectScale();
            base.getDecodeService().decodePage(this, width, zoom, this);
        }
    }

    public void decodeComplete(final Bitmap bitmap)
    {
        base.getView().post(new Runnable()
        {
            public void run()
            {
                setBitmap(bitmap);
                invalidateFlag = false;
                setDecodingNow(false);

                page.updateAspectRatio();

                invalidateChildren();
            }
        });
    }

    private RectF evaluatePageSliceBounds(RectF localPageSliceBounds, PageTreeNode parent) {
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

    private void setBitmap(Bitmap bitmap) {
        if (bitmap != null && bitmap.getWidth() == -1 && bitmap.getHeight() == -1) {
            return;
        }
        if (this.bitmap != bitmap) {
            if (bitmap != null) {
                if (this.bitmap != null) {
                    this.bitmap.recycle();
                }
                bitmapWeakReference = new SoftReference<Bitmap>(bitmap);
                base.getView().postInvalidate();
            }
            this.bitmap = bitmap;
        }
    }

    private boolean setDecodingNow(boolean decodingNow) {
        if (this.decodingNow.compareAndSet(!decodingNow, decodingNow)) {
            if (decodingNow) {
                base.getDecodingProgressModel().increase();
            } else {
                base.getDecodingProgressModel().decrease();
            }
            return true;
        }
        return false;
    }

    private Rect getTargetRect() {
        if (targetRect == null) {
            matrix.reset();
            matrix.postScale(page.getBounds().width() * page.getTargetRectScale(), page.getBounds().height());
            matrix.postTranslate(page.getBounds().left - page.getBounds().width() * page.getTargetTranslate(), page.getBounds().top);
            RectF targetRectF = new RectF();
            matrix.mapRect(targetRectF, pageSliceBounds);
            targetRect = new Rect((int) targetRectF.left, (int) targetRectF.top, (int) targetRectF.right, (int) targetRectF.bottom);
        }
        return targetRect;
    }

    private void stopDecodingThisNode(String reason)
    {
        if (setDecodingNow(false))
        {
            base.getDecodeService().stopDecoding(this, reason);
        }
    }

    private boolean isHiddenByChildren() {
        if (children == null) {
            return false;
        }
        for (PageTreeNode child : children) {
            if (child.getBitmap() == null) {
                return false;
            }
        }
        return true;
    }

    private void recycleChildren() {
        if (children == null) {
            return;
        }
        for (PageTreeNode child : children) {
            child.recycle();
        }
        if (!childrenContainBitmaps()) {
            children = null;
        }
    }

    private boolean containsBitmaps() {
        return getBitmap() != null || childrenContainBitmaps();
    }

    private boolean childrenContainBitmaps() {
        if (children == null) {
            return false;
        }
        for (PageTreeNode child : children) {
            if (child.containsBitmaps()) {
                return true;
            }
        }
        return false;
    }

    private void recycle() {
        stopDecodingThisNode("node recycling");
        setBitmap(null);
        if (children != null) {
            for (PageTreeNode child : children) {
                child.recycle();
            }
        }
    }

    private boolean isVisibleAndNotHiddenByChildren() {
        return isVisible() && !isHiddenByChildren();
    }

    public int getPageIndex() {
        return page.getIndex();
    }


    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((page == null) ? 0 : page.getIndex());
        result = prime * result + ((pageSliceBounds == null) ? 0 : pageSliceBounds.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj instanceof PageTreeNode)
        {
            PageTreeNode that = (PageTreeNode) obj;
            if (this.page == null)
            {
                return that.page == null;
            }
            return this.page.getIndex() == that.getPageIndex() && this.pageSliceBounds.equals(that.pageSliceBounds);
        }

        return false;
    }

    public String toString() {
      StringBuilder buf = new StringBuilder("PageTreeNode");
      buf.append("[");

      buf.append("page").append("=").append(page.getIndex());
      buf.append(", ");
      buf.append("rect").append("=").append(this.pageSliceBounds);
      buf.append(", ");
      buf.append("hasBitmap").append("=").append(getBitmap() != null);

      buf.append("]");
      return buf.toString();
    }

    public int getDocumentPageIndex() {
        return page.getDocumentPageIndex();
    }

}
