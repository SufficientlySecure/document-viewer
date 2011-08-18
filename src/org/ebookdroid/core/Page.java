package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;

import android.graphics.Canvas;
import android.graphics.RectF;

public class Page {

    private final int index;
    private RectF bounds;
    private final PageTreeNode node;
    private final IViewerActivity base;
    private float aspectRatio;
    private final int documentPage;
    private final PageType pageType;
    private boolean keptInMempory;
    private boolean visible;
    boolean recycled;

    public Page(final IViewerActivity base, final int index, final int documentPage, final PageType pt,
            final CodecPageInfo cpi) {
        this.base = base;
        this.index = index;
        this.documentPage = documentPage;
        this.pageType = pt != null ? pt : PageType.FULL_PAGE;

        setAspectRatio(cpi.getWidth(), cpi.getHeight());

        final boolean sliceLimit = base.getAppSettings().getSliceLimit();
        node = new PageTreeNode(base, pageType.getInitialRect(), this, 1, null, sliceLimit);
    }

    public void recycle() {
        recycled = true;
        node.recycle();
    }

    public float getPageHeight(final int mainWidth, final float zoom) {
        return mainWidth / getAspectRatio() * zoom;
    }

    public float getPageWidth(final int mainHeight, final float zoom) {
        return mainHeight * getAspectRatio() * zoom;
    }

    public int getTop() {
        return Math.round(getBounds().top);
    }

    public boolean draw(final Canvas canvas, RectF viewRect) {
        return draw(canvas, viewRect, false);
    }

    public boolean draw(final Canvas canvas, RectF viewRect, final boolean drawInvisible) {
        if (drawInvisible || isVisible()) {
            final PagePaint paint = base.getAppSettings().getNightMode() ? PagePaint.NIGHT : PagePaint.DAY;

            RectF bounds = new RectF(getBounds());
            bounds.offset(-viewRect.left, -viewRect.top);

            canvas.drawRect(bounds, paint.getFillPaint());

            canvas.drawText(base.getContext().getString(R.string.text_page) + " " + (getIndex() + 1), bounds.centerX(),
                    bounds.centerY(), paint.getTextPaint());

            node.draw(canvas, viewRect, new RectF(bounds), paint);

            canvas.drawLine(bounds.left, bounds.top, bounds.right, bounds.top, paint.getStrokePaint());
            canvas.drawLine(bounds.left, bounds.bottom, bounds.right, bounds.bottom, paint.getStrokePaint());
            return true;
        }
        return false;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    private void setAspectRatio(final float aspectRatio) {
        if (this.aspectRatio != aspectRatio) {
            this.aspectRatio = aspectRatio;
            base.getDocumentController().invalidatePageSizes();
        }
    }

    public void setAspectRatio(final CodecPage page) {
        if (page != null) {
            this.setAspectRatio(page.getWidth(), page.getHeight());
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void setAspectRatio(final int width, final int height) {
        setAspectRatio((width / pageType.getWidthScale()) / height);
    }

    public void setBounds(final RectF pageBounds) {
        bounds = pageBounds;
        node.invalidateNodeBounds();
    }

    public boolean isKeptInMemory() {
        return keptInMempory || visible;
    }

    private boolean calculateKeptInMemory() {
        int current = base.getDocumentModel().getCurrentViewPageIndex();
        int inMemory = (int) Math.ceil(base.getAppSettings().getPagesInMemory() / 2.0);
        return (current - inMemory <= this.index) && (this.index <= current + inMemory);
    }

    public void updateVisibility() {
        if (!recycled) {
            keptInMempory = calculateKeptInMemory();
            visible = base.getDocumentController().isPageVisible(this);
            node.updateVisibility();
        }
    }

    public void invalidate() {
        if (!recycled) {
            keptInMempory = calculateKeptInMemory();
            visible = base.getDocumentController().isPageVisible(this);
            node.invalidate();
        }
    }

    public RectF getBounds() {
        return bounds;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder("Page");
        buf.append("[");

        buf.append("index").append("=").append(index);
        buf.append(", ");
        buf.append("bounds").append("=").append(bounds);
        buf.append(", ");
        buf.append("aspectRatio").append("=").append(aspectRatio);
        buf.append(", ");
        buf.append("type").append("=").append(pageType.name());
        buf.append("]");
        return buf.toString();
    }

    public int getDocumentPageIndex() {
        return documentPage;
    }

    public float getTargetRectScale() {
        return pageType.getWidthScale();
    }

    public float getTargetTranslate() {
        return pageType.getLeftPos();
    }

}
