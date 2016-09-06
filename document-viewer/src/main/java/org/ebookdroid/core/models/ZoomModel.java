package org.ebookdroid.core.models;

import android.graphics.PointF;
import android.support.annotation.Nullable;

import org.ebookdroid.core.events.ZoomListener;

import org.emdev.utils.MathUtils;
import org.emdev.utils.listeners.ListenerProxy;

public class ZoomModel extends ListenerProxy {

    public static final float MIN_ZOOM = 1.0f;
    public static final float MAX_ZOOM = 32.0f;

    private static int ZOOM_ROUND_FACTOR = 0;

    private float initialZoom = MIN_ZOOM;
    private float currentZoom = MIN_ZOOM;

    private boolean isCommited;

    public ZoomModel() {
        super(ZoomListener.class);
    }

    public void initZoom(final float zoom) {
        this.initialZoom = this.currentZoom = adjust(zoom);
        isCommited = true;
    }

    public void setZoom(final float zoom) {
        setZoom(zoom, null);
    }

    public void setZoom(final float zoom, @Nullable PointF center) {
        setZoom(zoom, false, center);
        final float newZoom = adjust(zoom);
        final float oldZoom = this.currentZoom;
        if (newZoom != oldZoom) {
            isCommited = false;
            this.currentZoom = newZoom;
            this.<ZoomListener> getListener().zoomChanged(oldZoom, newZoom, false, center);
        }
    }

    public void setZoom(final float zoom, final boolean commitImmediately) {
        setZoom(zoom, commitImmediately, null);
    }

    public void setZoom(final float zoom, final boolean commitImmediately, @Nullable PointF center) {
        final float newZoom = adjust(zoom);
        final float oldZoom = this.currentZoom;
        if (newZoom != oldZoom || commitImmediately) {
            isCommited = commitImmediately;
            this.currentZoom = newZoom;
            this.<ZoomListener> getListener().zoomChanged(oldZoom, newZoom, commitImmediately, center);
            if (commitImmediately) {
                this.initialZoom = this.currentZoom;
            }
        }
    }

    public void scaleZoom(final float factor, PointF center) {
        setZoom(currentZoom * factor, false, center);
    }

    public void scaleAndCommitZoom(final float factor) {
        setZoom(currentZoom * factor, true);
    }

    public float getZoom() {
        return currentZoom;
    }

    public void commit() {
        if (!isCommited) {
            isCommited = true;
            this.<ZoomListener> getListener().zoomChanged(initialZoom, currentZoom, true, null);
            initialZoom = currentZoom;
        }
    }

    float adjust(final float zoom) {
        return MathUtils.adjust(ZOOM_ROUND_FACTOR <= 0 ? zoom : MathUtils.round(zoom, ZOOM_ROUND_FACTOR), MIN_ZOOM,
                MAX_ZOOM);
    }

}
