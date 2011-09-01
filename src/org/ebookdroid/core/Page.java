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

    public Page(final IViewerActivity base, final PageIndex index, final PageType pt, final CodecPageInfo cpi) {
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

    public boolean draw(final Canvas canvas, final ViewState viewState) {
        return draw(canvas, viewState, false);
    }

    public boolean draw(final Canvas canvas, final ViewState viewState, final boolean drawInvisible) {
        if (drawInvisible || isVisible(viewState)) {
            final PagePaint paint = SettingsManager.getAppSettings().getNightMode() ? PagePaint.NIGHT : PagePaint.DAY;

            final RectF bounds = new RectF(viewState.getBounds(this));
            final RectF nodesBounds = new RectF(bounds);
            bounds.offset(-viewState.viewRect.left, -viewState.viewRect.top);

            canvas.drawRect(bounds, paint.fillPaint);

            final TextPaint textPaint = paint.textPaint;
            textPaint.setTextSize(24 * base.getZoomModel().getZoom());
            canvas.drawText(base.getContext().getString(R.string.text_page) + " " + (index.viewIndex + 1),
                    bounds.centerX(), bounds.centerY(), textPaint);

            nodes.root.draw(canvas, viewState, nodesBounds, paint);

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

    public boolean isVisible(final ViewState viewState) {
        return viewState.isPageVisible(this);
    }

    public boolean isKeptInMemory(final ViewState viewState) {
        return isKeptInMemoryImpl(viewState) || isVisible(viewState);
    }

    private boolean isKeptInMemoryImpl(final ViewState viewState) {
        final int current = viewState.currentIndex;
        final int inMemory = (int) Math.ceil(SettingsManager.getAppSettings().getPagesInMemory() / 2.0);
        return (current - inMemory <= this.index.viewIndex) && (this.index.viewIndex <= current + inMemory);
    }

    public boolean onZoomChanged(final float oldZoom, final ViewState viewState, final List<PageTreeNode> nodesToDecode) {
        if (!recycled) {
            return nodes.root.onZoomChanged(oldZoom, viewState, viewState.getBounds(this), nodesToDecode);
        }
        return false;
    }

    public boolean onPositionChanged(final ViewState viewState, final List<PageTreeNode> nodesToDecode) {
        if (!recycled) {
            return nodes.root.onPositionChanged(viewState, viewState.getBounds(this), nodesToDecode);
        }
        return false;
    }

    public RectF getBounds(final float zoom) {
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
