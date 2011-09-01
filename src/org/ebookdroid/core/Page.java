package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.utils.MathUtils;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.text.TextPaint;

import java.util.List;

public class Page {

    public final PageIndex index;

    final IViewerActivity base;
    final PageTree nodes;

    RectF bounds;
    float aspectRatio;
    final PageType pageType;
    boolean recycled;
    boolean lowMemory;
    private float storedZoom;
    private RectF zoomedBounds;

    boolean nativeResolution;

    public Page(final IViewerActivity base, PageIndex index, final PageType pt, final CodecPageInfo cpi) {
        this.base = base;
        this.index = index;
        this.pageType = pt != null ? pt : PageType.FULL_PAGE;
        this.bounds = new RectF(0, 0, cpi.getWidth(), cpi.getHeight());

        setAspectRatio(cpi.getWidth(), cpi.getHeight());

        lowMemory = SettingsManager.getAppSettings().getLowMemory();
        nativeResolution = lowMemory ? false : SettingsManager.getAppSettings().getNativeResolution();
        nodes = new PageTree(this);
    }

    public void recycle() {
        recycled = true;
        nodes.recycle();
    }

    public int getTop() {
        return Math.round(getBounds().top);
    }

    public boolean draw(final Canvas canvas, RectF viewRect) {
        return draw(canvas, viewRect, false);
    }

    public boolean draw(final Canvas canvas, RectF viewRect, final boolean drawInvisible) {
        if (drawInvisible || isVisible()) {
            final PagePaint paint = SettingsManager.getAppSettings().getNightMode() ? PagePaint.NIGHT : PagePaint.DAY;

            RectF bounds = new RectF(getBounds());
            RectF nodesBounds = new RectF(bounds);
            bounds.offset(-viewRect.left, -viewRect.top);

            canvas.drawRect(bounds, paint.fillPaint);

            TextPaint textPaint = paint.textPaint;
            textPaint.setTextSize(24 * base.getZoomModel().getZoom());
            canvas.drawText(base.getContext().getString(R.string.text_page) + " " + (index.viewIndex + 1),
                    bounds.centerX(), bounds.centerY(), textPaint);

            nodes.root.draw(canvas, viewRect, nodesBounds, paint);

            canvas.drawLine(bounds.left, bounds.top, bounds.right, bounds.top, paint.strokePaint);
            canvas.drawLine(bounds.left, bounds.bottom, bounds.right, bounds.bottom, paint.strokePaint);
            return true;
        }
        return false;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    private boolean setAspectRatio(final float aspectRatio) {
        if (this.aspectRatio != aspectRatio) {
            this.aspectRatio = aspectRatio;
            return true;
        }
        return false;
    }

    public boolean setAspectRatio(final CodecPage page) {
        if (page != null) {
            return this.setAspectRatio(page.getWidth(), page.getHeight());
        }
        return false;
    }

    public boolean setAspectRatio(final int width, final int height) {
        return setAspectRatio((width / pageType.getWidthScale()) / height);
    }

    public void setBounds(final RectF pageBounds) {
        storedZoom = 0.0f;
        zoomedBounds = null;
        bounds = pageBounds;
    }

    public boolean isVisible() {
        return base.getDocumentController().isPageVisible(this);
    }

    public boolean isKeptInMemory() {
        return isKeptInMemoryImpl() || isVisible();
    }

    private boolean isKeptInMemoryImpl() {
        IDocumentViewController dc = base.getDocumentController();
        if (dc != null) {
            int current = ((AbstractDocumentView) dc).getCurrentPage();
            int inMemory = (int) Math.ceil(SettingsManager.getAppSettings().getPagesInMemory() / 2.0);
            return (current - inMemory <= this.index.viewIndex) && (this.index.viewIndex <= current + inMemory);
        }
        return false;
    }

    public boolean onZoomChanged(float oldZoom, float newZoom, RectF viewRect, final List<PageTreeNode> nodesToDecode) {
        if (!recycled) {
            return nodes.root.onZoomChanged(oldZoom, newZoom, viewRect, this.getBounds(), nodesToDecode);
        }
        return false;
    }

    public boolean onPositionChanged(RectF viewRect, final List<PageTreeNode> nodesToDecode) {
        if (!recycled) {
            return nodes.root.onPositionChanged(viewRect, this.getBounds(), nodesToDecode);
        }
        return false;
    }

    public RectF getBounds() {
        float zoom = base.getZoomModel().getZoom();
        if (zoom != storedZoom) {
            storedZoom = zoom;
            zoomedBounds = MathUtils.zoom(bounds, zoom);
        }
        return zoomedBounds;
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

    public float getTargetRectScale() {
        return pageType.getWidthScale();
    }

    public float getTargetTranslate() {
        return pageType.getLeftPos();
    }

}
